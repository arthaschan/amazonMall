package com.yudao.module.amazon.common.core;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception hierarchy for Amazon SP-API errors.
 *
 * <p>Subclasses map to specific HTTP status families:
 * <ul>
 *   <li>{@link SpApiAuthException}       &rarr; 401 Unauthorized / token issues</li>
 *   <li>{@link SpApiRateLimitException}  &rarr; 429 Too Many Requests</li>
 *   <li>{@link SpApiServerException}     &rarr; 500/502/503/504 Server errors</li>
 *   <li>{@link SpApiClientException}     &rarr; 4xx (except 401, 429) client errors</li>
 * </ul>
 */
public class SpApiException extends RuntimeException {

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
     */
    public static SpApiException fromStatusCode(int statusCode, String requestId,
                                                 String body, String endpoint) {
        String msg = String.format("SP-API call to [%s] failed with status %d (requestId=%s)",
                endpoint, statusCode, requestId);

        switch (statusCode) {
            case 401:
                return new SpApiAuthException(msg, requestId, body, null);
            case 429:
                return new SpApiRateLimitException(msg, requestId, body, null, -1);
            case 500:
            case 502:
            case 503:
            case 504:
                return new SpApiServerException(msg, statusCode, requestId, body, null);
            default:
                return new SpApiClientException(msg, statusCode, requestId, body, null);
        }
    }

    // ── Subclasses ────────────────────────────────────────────────────────

    /**
     * 401 Unauthorized -- token expired, revoked, or invalid credentials.
     */
    public static class SpApiAuthException extends SpApiException {

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
     */
    public static class SpApiRateLimitException extends SpApiException {

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
     */
    public static class SpApiServerException extends SpApiException {

        public SpApiServerException(String message, int statusCode,
                                    String requestId, String responseBody,
                                    Throwable cause) {
            super(message, statusCode, requestId, responseBody, cause);
        }
    }

    /**
     * 4xx Client Error (excluding 401 and 429) -- malformed request.
     */
    public static class SpApiClientException extends SpApiException {

        public SpApiClientException(String message, int statusCode,
                                    String requestId, String responseBody,
                                    Throwable cause) {
            super(message, statusCode, requestId, responseBody, cause);
        }
    }

    // ── Diagnostic helpers ────────────────────────────────────────────────

    /**
     * Returns a structured map of diagnostic fields.
     */
    public Map<String, Object> toDiagnosticMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("exceptionClass", getClass().getSimpleName());
        map.put("statusCode", statusCode);
        map.put("requestId", requestId != null ? requestId : "N/A");
        map.put("timestamp", timestamp.toString());
        map.put("message", getMessage());
        return map;
    }

    @Override
    public String toString() {
        return String.format("%s{statusCode=%d, requestId='%s', timestamp=%s, message='%s'}",
                getClass().getSimpleName(), statusCode, requestId, timestamp, getMessage());
    }
}
