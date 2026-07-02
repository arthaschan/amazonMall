package com.yudao.module.amazon.common.core;

import java.time.Instant;
import java.util.Map;

/**
 * Sealed exception hierarchy for Amazon SP-API errors.
 *
 * <p>Using sealed classes ensures the compiler knows all possible SP-API
 * failure modes, which makes exhaustive {@code switch} expressions possible
 * in calling code.
 *
 * <p>Subclasses map to specific HTTP status families:
 * <ul>
 *   <li>{@link SpApiAuthException}       &rarr; 401 Unauthorized / token issues</li>
 *   <li>{@link SpApiRateLimitException}  &rarr; 429 Too Many Requests</li>
 *   <li>{@link SpApiServerException}     &rarr; 500/502/503/504 Server errors</li>
 *   <li>{@link SpApiClientException}     &rarr; 4xx (except 401, 429) client errors</li>
 * </ul>
 */
public sealed class SpApiException extends RuntimeException
        permits SpApiException.SpApiAuthException,
                SpApiException.SpApiRateLimitException,
                SpApiException.SpApiServerException,
                SpApiException.SpApiClientException {

    /** HTTP status code returned by Amazon, or -1 if the request never completed. */
    private final int statusCode;

    /** Amazon request ID from the {@code x-amzn-RequestId} header (for support tickets). */
    private final String requestId;

    /** Raw error body from Amazon, if available. */
    private final String responseBody;

    /** Timestamp when the error occurred. */
    private final Instant timestamp;

    protected SpApiException(String message, int statusCode, String requestId,
                             String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.requestId = requestId;
        this.responseBody = responseBody;
        this.timestamp = Instant.now();
    }

    public int getStatusCode()      { return statusCode; }
    public String getRequestId()    { return requestId; }
    public String getResponseBody() { return responseBody; }
    public Instant getTimestamp()   { return timestamp; }

    /**
     * Factory that maps an HTTP status code to the correct exception subtype.
     *
     * @param statusCode   HTTP status
     * @param requestId    Amazon request ID (nullable)
     * @param body         response body (nullable)
     * @param endpoint     the SP-API endpoint path (for diagnostics)
     * @return the appropriate {@link SpApiException} subtype
     */
    public static SpApiException fromStatusCode(int statusCode, String requestId,
                                                 String body, String endpoint) {
        String msg = "SP-API call to [%s] failed with status %d (requestId=%s)"
                .formatted(endpoint, statusCode, requestId);

        return switch (statusCode) {
            case 401      -> new SpApiAuthException(msg, requestId, body, null);
            case 429      -> new SpApiRateLimitException(msg, requestId, body, null, -1);
            case 500, 502,
                 503, 504 -> new SpApiServerException(msg, statusCode, requestId, body, null);
            default       -> new SpApiClientException(msg, statusCode, requestId, body, null);
        };
    }

    // ── Subclasses ────────────────────────────────────────────────────────

    /**
     * 401 Unauthorized -- token expired, revoked, or invalid credentials.
     *
     * <p>When this is thrown the caller should invalidate the cached access
     * token and attempt a refresh via {@link SpApiTokenRefresher}.
     */
    public static final class SpApiAuthException extends SpApiException {

        public SpApiAuthException(String message, String requestId,
                                  String responseBody, Throwable cause) {
            super(message, 401, requestId, responseBody, cause);
        }

        public SpApiAuthException(String message, Throwable cause) {
            super(message, 401, null, null, cause);
        }
    }

    /**
     * 429 Too Many Requests -- rate limit exceeded.
     *
     * <p>The {@code retryAfterSeconds} field carries the server-suggested
     * back-off when present (from the {@code Retry-After} header).
     */
    public static final class SpApiRateLimitException extends SpApiException {

        /** Seconds the server suggests waiting before retrying (-1 if unknown). */
        private final long retryAfterSeconds;

        public SpApiRateLimitException(String message, String requestId,
                                       String responseBody, Throwable cause,
                                       long retryAfterSeconds) {
            super(message, 429, requestId, responseBody, cause);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    /**
     * 5xx Server Error -- Amazon-side transient failure.
     *
     * <p>Always retryable with exponential backoff.
     */
    public static final class SpApiServerException extends SpApiException {

        public SpApiServerException(String message, int statusCode,
                                    String requestId, String responseBody,
                                    Throwable cause) {
            super(message, statusCode, requestId, responseBody, cause);
        }
    }

    /**
     * 4xx Client Error (excluding 401 and 429) -- malformed request, bad
     * parameters, insufficient permissions, etc.
     *
     * <p>Generally NOT retryable; the caller must fix the request.
     */
    public static final class SpApiClientException extends SpApiException {

        public SpApiClientException(String message, int statusCode,
                                    String requestId, String responseBody,
                                    Throwable cause) {
            super(message, statusCode, requestId, responseBody, cause);
        }
    }

    // ── Diagnostic helpers ────────────────────────────────────────────────

    /**
     * Returns a structured map of diagnostic fields suitable for structured
     * logging (SLF4J key-value pairs or JSON).
     */
    public Map<String, Object> toDiagnosticMap() {
        return Map.ofEntries(
                Map.entry("exceptionClass", getClass().getSimpleName()),
                Map.entry("statusCode",    statusCode),
                Map.entry("requestId",     requestId != null ? requestId : "N/A"),
                Map.entry("timestamp",     timestamp.toString()),
                Map.entry("message",       getMessage())
        );
    }

    @Override
    public String toString() {
        return "%s{statusCode=%d, requestId='%s', timestamp=%s, message='%s'}"
                .formatted(getClass().getSimpleName(), statusCode,
                        requestId, timestamp, getMessage());
    }
}
