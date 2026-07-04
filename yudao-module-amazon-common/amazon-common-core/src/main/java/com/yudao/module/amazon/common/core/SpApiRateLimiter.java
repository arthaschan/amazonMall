package com.yudao.module.amazon.common.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed token-bucket rate limiter for SP-API endpoints.
 *
 * <p>Amazon SP-API enforces per-endpoint rate limits using a token-bucket
 * algorithm. Each endpoint has a <em>burst capacity</em> (maximum tokens in
 * the bucket) and a <em>restore rate</em> (tokens added per second).
 *
 * <p>This implementation uses Redis + Lua scripting to ensure atomic
 * token consumption across multiple application instances. The Lua script
 * runs entirely within Redis, eliminating race conditions.
 *
 * <p>Rate limits can be configured per endpoint via
 * {@link AmazonProperties#getRateLimits()}, or dynamically updated from the
 * {@code x-amzn-RateLimit-Limit} response header that Amazon returns.
 *
 * <h3>Redis key layout</h3>
 * <pre>
 *   sp:rate:{sellerId}:{endpoint}  →  (tokens, lastRefillTimestamp)
 * </pre>
 *
 * @see <a href="https://developer-docs.amazon.com/sp-api/docs/usage-plans-and-rate-limits">
 *      SP-API Usage Plans and Rate Limits</a>
 */
@Component
public class SpApiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(SpApiRateLimiter.class);

    /** Default burst capacity when no endpoint-specific config is provided. */
    private static final double DEFAULT_BURST_CAPACITY = 20.0;

    /** Default restore rate (tokens per second). */
    private static final double DEFAULT_RESTORE_RATE = 2.0;

    /** Redis key TTL to auto-expire stale rate-limit entries. */
    private static final Duration KEY_TTL = Duration.ofHours(2);

    /**
     * Lua script for atomic token-bucket consumption.
     *
     * <p>Keys: [1] = bucket key
     * <p>Args: [1] = burstCapacity, [2] = restoreRate, [3] = now (epoch millis), [4] = tokensToConsume
     * <p>Returns: 0 = allowed, 1 = denied (with waitMs in the second element)
     */
    private static final String LUA_TOKEN_BUCKET =
            "local key = KEYS[1]\n"
            + "local burstCapacity = tonumber(ARGV[1])\n"
            + "local restoreRate = tonumber(ARGV[2])\n"
            + "local now = tonumber(ARGV[3])\n"
            + "local requested = tonumber(ARGV[4])\n"
            + "\n"
            + "local bucket = redis.call('HMGET', key, 'tokens', 'lastRefill')\n"
            + "local tokens = tonumber(bucket[1])\n"
            + "local lastRefill = tonumber(bucket[2])\n"
            + "\n"
            + "if tokens == nil then\n"
            + "    -- First call: bucket starts full\n"
            + "    tokens = burstCapacity\n"
            + "    lastRefill = now\n"
            + "end\n"
            + "\n"
            + "-- Refill tokens based on elapsed time\n"
            + "local elapsed = (now - lastRefill) / 1000.0\n"
            + "tokens = math.min(burstCapacity, tokens + elapsed * restoreRate)\n"
            + "\n"
            + "if tokens >= requested then\n"
            + "    -- Consume and allow\n"
            + "    tokens = tokens - requested\n"
            + "    redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', now)\n"
            + "    redis.call('EXPIRE', key, 7200)\n"
            + "    return {0, 0}\n"
            + "else\n"
            + "    -- Denied: compute wait time\n"
            + "    local deficit = requested - tokens\n"
            + "    local waitMs = math.ceil(deficit / restoreRate * 1000)\n"
            + "    redis.call('EXPIRE', key, 7200)\n"
            + "    return {1, waitMs}\n"
            + "end";

    private final StringRedisTemplate redisTemplate;
    private final AmazonProperties properties;
    private final DefaultRedisScript<List<Long>> tokenBucketScript;

    /**
     * Local cache of dynamically-learned rate limits from response headers.
     * Key = endpoint path, value = rate limit config.
     */
    private final Map<String, AmazonProperties.RateLimitConfig> dynamicLimits = new ConcurrentHashMap<>();

    public SpApiRateLimiter(StringRedisTemplate redisTemplate, AmazonProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;

        this.tokenBucketScript = new DefaultRedisScript<>();
        this.tokenBucketScript.setScriptText(LUA_TOKEN_BUCKET);
        this.tokenBucketScript.setResultType((Class<List<Long>>) (Class<?>) List.class);

        log.info("SP-API rate limiter initialised with {} configured endpoint overrides",
                properties.getRateLimits().size());
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Attempts to acquire a rate-limit token for the given seller and endpoint.
     *
     * <p>If the bucket is empty, this method blocks (sleeps) until a token
     * becomes available.
     *
     * @param sellerId the Amazon seller identifier (for per-seller bucketing)
     * @param endpoint the API endpoint path (e.g. {@code "/orders/v0/orders"})
     */
    public void acquire(String sellerId, String endpoint) {
        while (true) {
            long waitMs = tryAcquire(sellerId, endpoint);
            if (waitMs <= 0) {
                return; // Token acquired
            }

            log.debug("Rate limited on [{}] for seller [{}], waiting {}ms",
                    endpoint, sellerId, waitMs);
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Rate limit wait interrupted for [{}]", endpoint);
                return;
            }
        }
    }

    /**
     * Tries to acquire a single token. Returns 0 on success, or the number of
     * milliseconds to wait before retrying.
     *
     * @param sellerId the Amazon seller identifier
     * @param endpoint the API endpoint path
     * @return 0 if acquired, otherwise wait time in milliseconds
     */
    public long tryAcquire(String sellerId, String endpoint) {
        return tryAcquire(sellerId, endpoint, 1);
    }

    /**
     * Tries to acquire {@code tokens} tokens from the bucket.
     *
     * @param sellerId the Amazon seller identifier
     * @param endpoint the API endpoint path
     * @param tokens   number of tokens to consume
     * @return 0 if acquired, otherwise wait time in milliseconds
     */
    public long tryAcquire(String sellerId, String endpoint, int tokens) {
        String key = "sp:rate:" + sellerId + ":" + endpoint;
        AmazonProperties.RateLimitConfig config = resolveConfig(endpoint);

        List<Long> keys = Collections.singletonList(key);
        List<Object> args = Arrays.asList(
                config.getBurstCapacity(),
                config.getRestoreRatePerSecond(),
                System.currentTimeMillis(),
                tokens
        );

        try {
            List<Long> result = redisTemplate.execute(tokenBucketScript, keys, args.toArray());

            if (result == null || result.isEmpty()) {
                log.warn("Redis Lua script returned null/empty for [{}], allowing request", endpoint);
                return 0; // Fail-open: allow request if Redis is unavailable
            }

            long denied = result.get(0);
            if (denied == 0) {
                return 0; // Allowed
            }

            long waitMs = result.size() > 1 ? result.get(1) : 1000L;
            return waitMs;

        } catch (Exception e) {
            // Fail-open: if Redis is down, let the request through and rely on
            // Amazon's server-side rate limiting + our retry policy
            log.warn("Redis unavailable for rate limiting [{}], failing open: {}",
                    endpoint, e.getMessage());
            return 0;
        }
    }

    /**
     * Updates the rate limit for an endpoint based on the
     * {@code x-amzn-RateLimit-Limit} response header from Amazon.
     *
     * <p>This allows the limiter to adapt dynamically to Amazon's
     * server-side rate-limit adjustments.
     *
     * @param endpoint   the API endpoint path
     * @param headerValue the value of the {@code x-amzn-RateLimit-Limit} header
     */
    public void updateFromResponseHeader(String endpoint, String headerValue) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return;
        }

        try {
            double restoreRate = Double.parseDouble(headerValue.trim());
            if (restoreRate > 0) {
                // Assume burst = restoreRate * 10 (typical Amazon ratio)
                double burstCapacity = restoreRate * 10;
                dynamicLimits.put(endpoint,
                        new AmazonProperties.RateLimitConfig(burstCapacity, restoreRate));
                log.debug("Updated rate limit for [{}]: burst={}, restore={}/s",
                        endpoint, burstCapacity, restoreRate);
            }
        } catch (NumberFormatException e) {
            log.debug("Could not parse x-amzn-RateLimit-Limit header: '{}'", headerValue);
        }
    }

    // ── Configuration resolution ──────────────────────────────────────────

    /**
     * Resolves the rate-limit config for an endpoint. Priority:
     * <ol>
     *   <li>Statically configured limits in {@code application.yml}</li>
     *   <li>Dynamically learned limits from response headers</li>
     *   <li>Default values</li>
     * </ol>
     */
    private AmazonProperties.RateLimitConfig resolveConfig(String endpoint) {
        // 1. Check static configuration
        AmazonProperties.RateLimitConfig staticConfig = properties.getRateLimits().get(endpoint);
        if (staticConfig != null) {
            return staticConfig;
        }

        // 2. Check dynamic (learned from headers)
        AmazonProperties.RateLimitConfig dynamicConfig = dynamicLimits.get(endpoint);
        if (dynamicConfig != null) {
            return dynamicConfig;
        }

        // 3. Try pattern matching on static config (e.g. "/orders/*" matches "/orders/v0/orders")
        for (Map.Entry<String, AmazonProperties.RateLimitConfig> entry : properties.getRateLimits().entrySet()) {
            if (matchesPattern(endpoint, entry.getKey())) {
                return entry.getValue();
            }
        }

        // 4. Default
        return new AmazonProperties.RateLimitConfig(DEFAULT_BURST_CAPACITY, DEFAULT_RESTORE_RATE);
    }

    /**
     * Simple wildcard pattern matching for endpoint paths.
     * Supports {@code *} as a trailing wildcard (e.g. {@code "/orders/*"}).
     */
    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }
}
