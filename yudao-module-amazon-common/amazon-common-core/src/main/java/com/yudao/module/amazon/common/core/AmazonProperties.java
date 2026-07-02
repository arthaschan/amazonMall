package com.yudao.module.amazon.common.core;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Amazon SP-API configuration properties.
 *
 * <p>Binds to {@code amazon.sp-api.*} in application.yml. All secret values
 * are injected from environment variables or a secrets manager -- never
 * hard-code them in configuration files.
 *
 * <p>Regional endpoint mapping follows the official SP-API documentation:
 * <ul>
 *   <li>NA  &rarr; us-east-1 &rarr; sellingpartnerapi-na.amazon.com</li>
 *   <li>EU  &rarr; eu-west-1 &rarr; sellingpartnerapi-eu.amazon.com</li>
 *   <li>FE  &rarr; us-west-2 &rarr; sellingpartnerapi-fe.amazon.com</li>
 * </ul>
 *
 * @see <a href="https://developer-docs.amazon.com/sp-api/docs/sp-api-endpoints">SP-API Endpoints</a>
 */
@Validated
@ConfigurationProperties(prefix = "amazon.sp-api")
public class AmazonProperties {

    // ── Application identity ──────────────────────────────────────────────

    /** Amazon application ID (from Seller Central / Developer Central). */
    @NotBlank
    private String appId;

    /** Login with Amazon (LWA) client ID. */
    @NotBlank
    private String clientId;

    /** Login with Amazon (LWA) client secret. */
    @NotBlank
    private String clientSecret;

    // ── AWS credentials (for Signature V4 signing) ────────────────────────

    /** AWS IAM access key ID used to sign SP-API requests. */
    @NotBlank
    private String awsAccessKeyId;

    /** AWS IAM secret access key used to sign SP-API requests. */
    @NotBlank
    private String awsSecretAccessKey;

    /** AWS IAM role ARN to assume when signing requests (optional, for STS). */
    private String awsRoleArn;

    // ── OAuth / redirect ──────────────────────────────────────────────────

    /** OAuth2 redirect URI registered with the SP-API application. */
    @NotBlank
    private String redirectUri;

    // ── LWA token endpoint ────────────────────────────────────────────────

    /**
     * LWA token endpoint URL.
     * Defaults to the production endpoint; override for sandbox testing.
     */
    private String tokenEndpoint = "https://api.amazon.com/auth/o2/token";

    // ── Environment ───────────────────────────────────────────────────────

    /** When {@code true}, use sandbox endpoints instead of production. */
    private boolean sandbox = false;

    /** Default marketplace ID (e.g. {@code ATVPDKIKX0DER} for US). */
    private String defaultMarketplaceId = "ATVPDKIKX0DER";

    // ── Encryption ────────────────────────────────────────────────────────

    /**
     * AES-256-GCM key (Base64-encoded, 32 bytes) for encrypting stored
     * credentials (refresh tokens, client secrets). Sourced from
     * {@code AMAZON_CREDENTIALS_ENCRYPTION_KEY} environment variable.
     */
    private String encryptionKey;

    // ── Proxy (optional) ──────────────────────────────────────────────────

    /** HTTP/SOCKS proxy configuration for environments behind a firewall. */
    private Proxy proxy = new Proxy();

    // ── Token management ──────────────────────────────────────────────────

    /** Access-token cache TTL in seconds. Default 55 minutes (tokens expire at 60 min). */
    private int accessTokenTtlSeconds = 3300;

    /**
     * Buffer in seconds before actual expiry to trigger a proactive refresh.
     * Default 5 minutes.
     */
    private int tokenRefreshBufferSeconds = 300;

    // ── Rate limiting ─────────────────────────────────────────────────────

    /**
     * Per-endpoint rate-limit overrides.
     * Key = API path pattern (e.g. {@code "/orders/v0/orders"}),
     * value = max requests per second (burst capacity).
     *
     * <p>Endpoints not listed here fall back to Amazon's documented burst /
     * restore rates returned in the {@code x-amzn-RateLimit-Limit} header.
     */
    private Map<String, RateLimitConfig> rateLimits = Map.of();

    // ── Retry ─────────────────────────────────────────────────────────────

    /** Maximum number of retry attempts for transient failures. */
    private int maxRetries = 3;

    /** Initial backoff delay in milliseconds for exponential backoff. */
    private long initialBackoffMs = 500;

    /** Maximum backoff cap in milliseconds. */
    private long maxBackoffMs = 30_000;

    // ── Regional endpoint resolution ──────────────────────────────────────

    /**
     * Resolves the SP-API regional endpoint for the given AWS region.
     *
     * @param awsRegion one of {@code us-east-1}, {@code eu-west-1}, {@code us-west-2}
     * @return the production or sandbox endpoint URL
     */
    public String resolveEndpoint(String awsRegion) {
        String base = switch (awsRegion) {
            case "us-east-1" -> sandbox
                    ? "https://sandbox.sellingpartnerapi-na.amazon.com"
                    : "https://sellingpartnerapi-na.amazon.com";
            case "eu-west-1" -> sandbox
                    ? "https://sandbox.sellingpartnerapi-eu.amazon.com"
                    : "https://sellingpartnerapi-eu.amazon.com";
            case "us-west-2" -> sandbox
                    ? "https://sandbox.sellingpartnerapi-fe.amazon.com"
                    : "https://sellingpartnerapi-fe.amazon.com";
            default -> throw new IllegalArgumentException(
                    "Unsupported AWS region for SP-API: " + awsRegion);
        };
        return base;
    }

    /**
     * Maps an AWS region code to the Amazon region abbreviation.
     *
     * @param awsRegion e.g. {@code us-east-1}
     * @return {@code NA}, {@code EU}, or {@code FE}
     */
    public String resolveRegionCode(String awsRegion) {
        return switch (awsRegion) {
            case "us-east-1" -> "NA";
            case "eu-west-1" -> "EU";
            case "us-west-2" -> "FE";
            default -> throw new IllegalArgumentException(
                    "Unsupported AWS region: " + awsRegion);
        };
    }

    // ── Nested records ────────────────────────────────────────────────────

    /** Proxy configuration record. */
    public static class Proxy {
        private boolean enabled = false;
        private String type = "HTTP"; // HTTP or SOCKS
        private String host;
        private int port;
        private String username;
        private String password;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /** Per-endpoint rate-limit configuration record. */
    public record RateLimitConfig(
            /** Maximum burst capacity (tokens in the bucket). */
            double burstCapacity,
            /** Tokens restored per second. */
            double restoreRatePerSecond
    ) {}

    // ── Standard getters/setters ──────────────────────────────────────────

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getAwsAccessKeyId() { return awsAccessKeyId; }
    public void setAwsAccessKeyId(String awsAccessKeyId) { this.awsAccessKeyId = awsAccessKeyId; }

    public String getAwsSecretAccessKey() { return awsSecretAccessKey; }
    public void setAwsSecretAccessKey(String awsSecretAccessKey) { this.awsSecretAccessKey = awsSecretAccessKey; }

    public String getAwsRoleArn() { return awsRoleArn; }
    public void setAwsRoleArn(String awsRoleArn) { this.awsRoleArn = awsRoleArn; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getTokenEndpoint() { return tokenEndpoint; }
    public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }

    public boolean isSandbox() { return sandbox; }
    public void setSandbox(boolean sandbox) { this.sandbox = sandbox; }

    public String getDefaultMarketplaceId() { return defaultMarketplaceId; }
    public void setDefaultMarketplaceId(String defaultMarketplaceId) { this.defaultMarketplaceId = defaultMarketplaceId; }

    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }

    public Proxy getProxy() { return proxy; }
    public void setProxy(Proxy proxy) { this.proxy = proxy; }

    public int getAccessTokenTtlSeconds() { return accessTokenTtlSeconds; }
    public void setAccessTokenTtlSeconds(int accessTokenTtlSeconds) { this.accessTokenTtlSeconds = accessTokenTtlSeconds; }

    public int getTokenRefreshBufferSeconds() { return tokenRefreshBufferSeconds; }
    public void setTokenRefreshBufferSeconds(int tokenRefreshBufferSeconds) { this.tokenRefreshBufferSeconds = tokenRefreshBufferSeconds; }

    public Map<String, RateLimitConfig> getRateLimits() { return rateLimits; }
    public void setRateLimits(Map<String, RateLimitConfig> rateLimits) { this.rateLimits = rateLimits; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long getInitialBackoffMs() { return initialBackoffMs; }
    public void setInitialBackoffMs(long initialBackoffMs) { this.initialBackoffMs = initialBackoffMs; }

    public long getMaxBackoffMs() { return maxBackoffMs; }
    public void setMaxBackoffMs(long maxBackoffMs) { this.maxBackoffMs = maxBackoffMs; }
}
