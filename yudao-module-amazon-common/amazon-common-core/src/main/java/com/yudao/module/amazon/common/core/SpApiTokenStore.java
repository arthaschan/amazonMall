package com.yudao.module.amazon.common.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Manages SP-API access tokens and refresh tokens with a two-tier strategy:
 *
 * <ul>
 *   <li><strong>Access tokens</strong> (short-lived, ~60 min): cached in Redis
 *       with a TTL that expires before the Amazon-issued expiry. Fast lookups
 *       for every API call.</li>
 *   <li><strong>Refresh tokens</strong> (long-lived): persisted encrypted in
 *       MySQL. Retrieved only when the access token needs refreshing.</li>
 * </ul>
 *
 * <h3>Token lifecycle</h3>
 * <pre>
 *  1. Seller authorises  →  auth code exchanged for refresh + access tokens
 *  2. Refresh token      →  encrypted and stored in MySQL
 *  3. Access token       →  cached in Redis with TTL
 *  4. API call           →  getAccessToken() returns cached or auto-refreshes
 *  5. Token expires      →  getAccessToken() detects expiry, refreshes, re-caches
 * </pre>
 *
 * <h3>Redis key layout</h3>
 * <pre>
 *   sp:token:access:{sellerId}  →  access token string
 *   sp:token:expiry:{sellerId}  →  expiry epoch-millis string
 * </pre>
 */
@Component
public class SpApiTokenStore {

    private static final Logger log = LoggerFactory.getLogger(SpApiTokenStore.class);

    /** Redis key prefix for access tokens. */
    private static final String REDIS_ACCESS_KEY = "sp:token:access:";

    /** Redis key prefix for token expiry timestamps. */
    private static final String REDIS_EXPIRY_KEY = "sp:token:expiry:";

    private final StringRedisTemplate redisTemplate;
    private final AmazonProperties properties;
    private final AmazonCredentialEncryptor encryptor;
    private final SpApiTokenRefresher tokenRefresher;
    private final DataSource dataSource;

    public SpApiTokenStore(StringRedisTemplate redisTemplate,
                           AmazonProperties properties,
                           AmazonCredentialEncryptor encryptor,
                           SpApiTokenRefresher tokenRefresher,
                           DataSource dataSource) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.encryptor = encryptor;
        this.tokenRefresher = tokenRefresher;
        this.dataSource = dataSource;
    }

    // ── Token records ─────────────────────────────────────────────────────

    /**
     * Snapshot of a seller's access token with metadata.
     *
     * @param sellerId    the Amazon seller identifier
     * @param accessToken the access token string
     * @param expiresAt   when the token expires
     */
    public record AccessTokenEntry(String sellerId, String accessToken, Instant expiresAt) {

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public boolean isExpiringSoon(Duration buffer) {
            return Instant.now().plus(buffer).isAfter(expiresAt);
        }
    }

    /**
     * Persisted refresh token record (matches the database schema).
     *
     * @param sellerId     the Amazon seller identifier
     * @param refreshToken the encrypted refresh token
     * @param updatedAt    when the record was last updated
     */
    public record RefreshTokenEntry(String sellerId, String refreshToken, Instant updatedAt) {}

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Retrieves a valid access token for the given seller, refreshing
     * automatically if the cached token is expired or about to expire.
     *
     * <p>This is the primary method that all SP-API calls should use to
     * obtain their access token.
     *
     * @param sellerId the Amazon seller identifier
     * @return a valid access token string
     * @throws SpApiException.SpApiAuthException if no refresh token is available
     *         or the refresh fails
     */
    public String getAccessToken(String sellerId) {
        // 1. Try Redis cache first
        AccessTokenEntry cached = getCachedAccessToken(sellerId);
        Duration buffer = Duration.ofSeconds(properties.getTokenRefreshBufferSeconds());

        if (cached != null && !cached.isExpiringSoon(buffer)) {
            log.debug("Using cached access token for seller [{}] (expires {})",
                    sellerId, cached.expiresAt());
            return cached.accessToken();
        }

        // 2. Cache miss or expired: refresh
        log.info("Access token for seller [{}] is missing/expiring, refreshing...", sellerId);
        return refreshAndCacheAccessToken(sellerId);
    }

    /**
     * Explicitly invalidates the cached access token for a seller.
     * Call this after receiving a 401 to force a refresh on the next call.
     *
     * @param sellerId the Amazon seller identifier
     */
    public void invalidateAccessToken(String sellerId) {
        redisTemplate.delete(REDIS_ACCESS_KEY + sellerId);
        redisTemplate.delete(REDIS_EXPIRY_KEY + sellerId);
        log.info("Invalidated access token cache for seller [{}]", sellerId);
    }

    /**
     * Stores a refresh token for a seller (encrypts before persisting to MySQL).
     *
     * @param sellerId     the Amazon seller identifier
     * @param refreshToken the plaintext refresh token from the OAuth flow
     */
    public void storeRefreshToken(String sellerId, String refreshToken) {
        String encrypted = encryptor.encrypt(refreshToken);
        persistRefreshToken(sellerId, encrypted);
        log.info("Stored encrypted refresh token for seller [{}]", sellerId);
    }

    /**
     * Retrieves the decrypted refresh token for a seller from MySQL.
     *
     * @param sellerId the Amazon seller identifier
     * @return the plaintext refresh token, or {@code null} if not found
     */
    public String getRefreshToken(String sellerId) {
        String encrypted = loadEncryptedRefreshToken(sellerId);
        if (encrypted == null) {
            return null;
        }
        return encryptor.decrypt(encrypted);
    }

    /**
     * Stores the access token and its expiry in the Redis cache, and also
     * persists the refresh token if one was returned.
     *
     * @param sellerId      the Amazon seller identifier
     * @param tokenResponse the token response from the LWA endpoint
     */
    public void storeTokens(String sellerId, SpApiTokenRefresher.TokenResponse tokenResponse) {
        // Cache the access token in Redis
        cacheAccessToken(sellerId, tokenResponse.accessToken(), tokenResponse.expiresIn());

        // Persist refresh token if a new one was returned (auth-code flow)
        if (tokenResponse.refreshToken() != null && !tokenResponse.refreshToken().isBlank()) {
            storeRefreshToken(sellerId, tokenResponse.refreshToken());
        }
    }

    // ── Redis operations ──────────────────────────────────────────────────

    /**
     * Reads the cached access token from Redis.
     *
     * @return the cached entry, or {@code null} if not present or expired
     */
    private AccessTokenEntry getCachedAccessToken(String sellerId) {
        try {
            String token = redisTemplate.opsForValue().get(REDIS_ACCESS_KEY + sellerId);
            String expiryStr = redisTemplate.opsForValue().get(REDIS_EXPIRY_KEY + sellerId);

            if (token != null && expiryStr != null) {
                Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(expiryStr));
                return new AccessTokenEntry(sellerId, token, expiresAt);
            }
        } catch (Exception e) {
            log.warn("Failed to read access token from Redis for seller [{}]: {}",
                    sellerId, e.getMessage());
        }
        return null;
    }

    /**
     * Caches an access token in Redis with an appropriate TTL.
     */
    private void cacheAccessToken(String sellerId, String accessToken, int expiresInSec) {
        try {
            // Use the configured TTL or the server-reported expiry, whichever is shorter
            int effectiveTtl = Math.min(expiresInSec, properties.getAccessTokenTtlSeconds());
            Duration ttl = Duration.ofSeconds(effectiveTtl);

            redisTemplate.opsForValue().set(REDIS_ACCESS_KEY + sellerId, accessToken, ttl);
            redisTemplate.opsForValue().set(
                    REDIS_EXPIRY_KEY + sellerId,
                    String.valueOf(Instant.now().plus(ttl).toEpochMilli()));

            log.debug("Cached access token for seller [{}] with TTL {}s", sellerId, effectiveTtl);
        } catch (Exception e) {
            log.warn("Failed to cache access token in Redis for seller [{}]: {}",
                    sellerId, e.getMessage());
        }
    }

    // ── Token refresh ─────────────────────────────────────────────────────

    /**
     * Refreshes the access token using the stored refresh token and caches it.
     */
    private String refreshAndCacheAccessToken(String sellerId) {
        String refreshToken = getRefreshToken(sellerId);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new SpApiException.SpApiAuthException(
                    "No refresh token found for seller [%s]. Re-authorisation required."
                            .formatted(sellerId),
                    null);
        }

        try {
            SpApiTokenRefresher.TokenResponse response =
                    tokenRefresher.refreshAccessTokenWithRetry(refreshToken, 2);

            cacheAccessToken(sellerId, response.accessToken(), response.expiresIn());

            log.info("Successfully refreshed access token for seller [{}] (expires in {}s)",
                    sellerId, response.expiresIn());
            return response.accessToken();

        } catch (SpApiException.SpApiAuthException e) {
            log.error("Failed to refresh access token for seller [{}]: {}", sellerId, e.getMessage());
            throw e;
        }
    }

    // ── MySQL persistence ─────────────────────────────────────────────────

    /**
     * Persists an encrypted refresh token to MySQL.
     *
     * <p>Uses JDBC directly to avoid a hard dependency on MyBatis-Plus or
     * other ORM frameworks. The table schema is:
     * <pre>{@code
     * CREATE TABLE amazon_refresh_token (
     *     seller_id      VARCHAR(64)  PRIMARY KEY,
     *     refresh_token  TEXT         NOT NULL,
     *     updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     *     created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
     * );
     * }</pre>
     */
    private void persistRefreshToken(String sellerId, String encryptedToken) {
        String sql = """
                INSERT INTO amazon_refresh_token (seller_id, refresh_token, updated_at)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE refresh_token = VALUES(refresh_token), updated_at = VALUES(updated_at)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sellerId);
            ps.setString(2, encryptedToken);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.executeUpdate();

        } catch (Exception e) {
            log.error("Failed to persist refresh token for seller [{}]: {}", sellerId, e.getMessage());
            throw new IllegalStateException("Failed to persist refresh token", e);
        }
    }

    /**
     * Loads the encrypted refresh token from MySQL.
     */
    private String loadEncryptedRefreshToken(String sellerId) {
        String sql = "SELECT refresh_token FROM amazon_refresh_token WHERE seller_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("refresh_token");
                }
            }

        } catch (Exception e) {
            log.error("Failed to load refresh token for seller [{}]: {}", sellerId, e.getMessage());
        }
        return null;
    }
}
