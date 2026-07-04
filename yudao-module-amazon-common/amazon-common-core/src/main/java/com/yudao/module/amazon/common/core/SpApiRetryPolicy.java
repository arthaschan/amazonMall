package com.yudao.module.amazon.common.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Retry policy for SP-API calls with exponential backoff and jitter.
 *
 * <p>Amazon SP-API uses a token-bucket rate-limiter on most endpoints and
 * returns 429/500/502/503/504 for transient failures. This policy
 * encapsulates the decision logic for whether and how long to wait before
 * retrying a failed request.
 *
 * <p>Backoff formula (with full jitter):
 * <pre>
 *   delay = random(0, min(maxBackoff, initialBackoff * 2^attempt))
 * </pre>
 *
 * <p>Inspired by the AWS Architecture Blog "Exponential Backoff and Jitter":
 * <a href="https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/">
 * Exponential Backoff and Jitter</a>.
 */
@Component
public class SpApiRetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(SpApiRetryPolicy.class);

    /** HTTP status codes that are safe to retry (transient failures). */
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    429,  // Too Many Requests (rate-limited)
                    500,  // Internal Server Error
                    502,  // Bad Gateway
                    503,  // Service Unavailable
                    504   // Gateway Timeout
            )));

    /** HTTP status codes that must NOT be retried (permanent client errors). */
    private static final Set<Integer> NON_RETRYABLE_STATUS_CODES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    400,  // Bad Request
                    401,  // Unauthorized (token refresh needed, not simple retry)
                    403,  // Forbidden
                    404,  // Not Found
                    409,  // Conflict
                    413,  // Payload Too Large
                    422   // Unprocessable Entity
            )));

    private final AmazonProperties properties;

    public SpApiRetryPolicy(AmazonProperties properties) {
        this.properties = properties;
    }

    // ── Core decision methods ─────────────────────────────────────────────

    /**
     * Determines whether a given HTTP status code is retryable.
     *
     * @param statusCode the HTTP status code
     * @return {@code true} if the request should be retried
     */
    public boolean isRetryable(int statusCode) {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }

    /**
     * Determines whether a given exception is retryable.
     *
     * @param ex the exception thrown during the SP-API call
     * @return {@code true} if the request should be retried
     */
    public boolean isRetryable(Exception ex) {
        return switch (ex) {
            case SpApiException.SpApiRateLimitException e  -> true;
            case SpApiException.SpApiServerException e     -> true;
            case SpApiException.SpApiAuthException e       -> false;  // token refresh, not retry
            case SpApiException.SpApiClientException e     -> false;  // permanent client error
            default                                        -> true;   // network errors, timeouts
        };
    }

    /**
     * Returns {@code true} if the given attempt number (0-based) is within the
     * configured maximum retry count.
     *
     * @param attempt the current attempt number (0 = first retry, 1 = second retry, etc.)
     * @return {@code true} if another attempt is allowed
     */
    public boolean shouldRetry(int attempt) {
        return attempt < properties.getMaxRetries();
    }

    // ── Backoff computation ───────────────────────────────────────────────

    /**
     * Computes the backoff delay in milliseconds for the given attempt using
     * exponential backoff with full jitter.
     *
     * @param attempt the current attempt number (0-based)
     * @return delay in milliseconds before the next retry
     */
    public long computeBackoffMs(int attempt) {
        long initialBackoff = properties.getInitialBackoffMs();
        long maxBackoff = properties.getMaxBackoffMs();

        // Exponential backoff: initialBackoff * 2^attempt
        long exponentialDelay = initialBackoff * (1L << attempt);
        long cappedDelay = Math.min(maxBackoff, exponentialDelay);

        // Full jitter: random value in [0, cappedDelay]
        long jitteredDelay = (long) (Math.random() * cappedDelay);

        log.debug("Retry attempt {} - backoff: {}ms (exponential: {}ms, capped: {}ms)",
                attempt, jitteredDelay, exponentialDelay, cappedDelay);

        return jitteredDelay;
    }

    /**
     * Computes the backoff delay, respecting the server's Retry-After header
     * when present (e.g. from a 429 response).
     *
     * @param attempt          the current attempt number (0-based)
     * @param retryAfterSeconds the server-suggested wait time in seconds, or -1 if absent
     * @return delay in milliseconds before the next retry
     */
    public long computeBackoffMs(int attempt, long retryAfterSeconds) {
        if (retryAfterSeconds > 0) {
            // Honour the server's Retry-After, but still cap at maxBackoff
            long serverSuggested = retryAfterSeconds * 1000L;
            long capped = Math.min(serverSuggested, properties.getMaxBackoffMs());
            log.debug("Retry attempt {} - using server Retry-After: {}s ({}ms)",
                    attempt, retryAfterSeconds, capped);
            return capped;
        }
        return computeBackoffMs(attempt);
    }

    // ── Convenience ───────────────────────────────────────────────────────

    /**
     * Sleeps for the computed backoff duration. Logs a warning if the thread
     * is interrupted.
     *
     * @param attempt the current attempt number (0-based)
     */
    public void sleepForBackoff(int attempt) {
        long delay = computeBackoffMs(attempt);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry backoff sleep interrupted on attempt {}", attempt);
        }
    }

    /**
     * Sleeps for the computed backoff duration, respecting Retry-After.
     *
     * @param attempt           the current attempt number (0-based)
     * @param retryAfterSeconds server-suggested wait time, or -1
     */
    public void sleepForBackoff(int attempt, long retryAfterSeconds) {
        long delay = computeBackoffMs(attempt, retryAfterSeconds);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry backoff sleep interrupted on attempt {}", attempt);
        }
    }

    // ── Configuration accessors ───────────────────────────────────────────

    public int getMaxRetries() {
        return properties.getMaxRetries();
    }

    public long getInitialBackoffMs() {
        return properties.getInitialBackoffMs();
    }

    public long getMaxBackoffMs() {
        return properties.getMaxBackoffMs();
    }
}
