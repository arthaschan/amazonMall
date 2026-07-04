package com.yudao.module.amazon.common.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Unified HTTP client for all Amazon SP-API calls.
 *
 * <p>This client encapsulates the full SP-API request pipeline:
 * <ol>
 *   <li><strong>Token injection</strong> -- retrieves a valid access token
 *       from {@link SpApiTokenStore} and adds the {@code x-amz-access-token}
 *       header.</li>
 *   <li><strong>AWS Signature V4 signing</strong> -- signs every request
 *       with the configured IAM credentials so Amazon can authenticate
 *       the calling application.</li>
 *   <li><strong>Rate limiting</strong> -- consults {@link SpApiRateLimiter}
 *       before sending to avoid hitting Amazon's token-bucket limits.</li>
 *   <li><strong>Retry with backoff</strong> -- retries on 429/500/502/503
 *       per {@link SpApiRetryPolicy}.</li>
 *   <li><strong>Request/response logging</strong> -- logs all requests at
 *       DEBUG level and errors at WARN/ERROR level.</li>
 *   <li><strong>Error handling</strong> -- maps HTTP errors to the
 *       {@link SpApiException} hierarchy.</li>
 * </ol>
 */
@Component
public class SpApiClient {

    private static final Logger log = LoggerFactory.getLogger(SpApiClient.class);

    /** Amazon service name for Signature V4. */
    private static final String SERVICE_NAME = "execute-api";

    /** AWS4-HMAC-SHA256 signing algorithm identifier. */
    private static final String AWS4_HMAC_SHA256 = "AWS4-HMAC-SHA256";

    /** Date format for Signature V4 (ISO 8601 basic). */
    private static final DateTimeFormatter ISO8601_BASIC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    /** Date format for the credential scope (date only). */
    private static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    /** JSON media type for OkHttp request bodies. */
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final AmazonProperties properties;
    private final SpApiTokenStore tokenStore;
    private final SpApiRateLimiter rateLimiter;
    private final SpApiRetryPolicy retryPolicy;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public SpApiClient(AmazonProperties properties,
                       SpApiTokenStore tokenStore,
                       SpApiRateLimiter rateLimiter,
                       SpApiRetryPolicy retryPolicy,
                       ObjectMapper objectMapper) {
        this.properties = properties;
        this.tokenStore = tokenStore;
        this.rateLimiter = rateLimiter;
        this.retryPolicy = retryPolicy;
        this.objectMapper = objectMapper;
        this.httpClient = buildHttpClient();
    }

    // ── Response wrapper ──────────────────────────────────────────────────

    /**
     * Immutable wrapper for an SP-API response.
     */
    public static class SpApiResponse {

        private final int statusCode;
        private final Map<String, String> headers;
        private final String body;
        private final JsonNode jsonBody;
        private final String requestId;

        public SpApiResponse(int statusCode, Map<String, String> headers,
                             String body, JsonNode jsonBody, String requestId) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
            this.jsonBody = jsonBody;
            this.requestId = requestId;
        }

        /** Whether the response indicates success (2xx). */
        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }

        public int getStatusCode()      { return statusCode; }
        public Map<String, String> getHeaders() { return headers; }
        public String getBody()          { return body; }
        public JsonNode getJsonBody()    { return jsonBody; }
        public String getRequestId()     { return requestId; }

        /** Extracts error message from the response body (best-effort). */
        public String getErrorMessage() {
            if (body == null || body.trim().isEmpty()) {
                return "HTTP " + statusCode;
            }
            // Try to extract from JSON error structure
            if (jsonBody != null && jsonBody.has("errors") && jsonBody.get("errors").isArray()) {
                JsonNode errors = jsonBody.get("errors");
                if (!errors.isEmpty() && errors.get(0).has("message")) {
                    return errors.get(0).get("message").asText();
                }
                if (!errors.isEmpty() && errors.get(0).has("code")) {
                    return errors.get(0).get("code").asText();
                }
            }
            return body;
        }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Sends a GET request to the SP-API.
     */
    public JsonNode get(String sellerId, String awsRegion, String path,
                        Map<String, String> queryParams) {
        return executeWithRetry(sellerId, awsRegion, "GET", path,
                queryParams, null).getJsonBody();
    }

    /**
     * Sends a POST request to the SP-API.
     */
    public JsonNode post(String sellerId, String awsRegion, String path, Object body) {
        return executeWithRetry(sellerId, awsRegion, "POST", path,
                null, body).getJsonBody();
    }

    /**
     * Sends a PUT request to the SP-API.
     */
    public JsonNode put(String sellerId, String awsRegion, String path, Object body) {
        return executeWithRetry(sellerId, awsRegion, "PUT", path,
                null, body).getJsonBody();
    }

    /**
     * Sends a DELETE request to the SP-API.
     */
    public JsonNode delete(String sellerId, String awsRegion, String path) {
        return executeWithRetry(sellerId, awsRegion, "DELETE", path,
                null, null).getJsonBody();
    }

    /**
     * Sends a PATCH request to the SP-API.
     */
    public JsonNode patch(String sellerId, String awsRegion, String path, Object body) {
        return executeWithRetry(sellerId, awsRegion, "PATCH", path,
                null, body).getJsonBody();
    }

    /**
     * Sends a GET request and returns the full response (including headers).
     */
    public SpApiResponse getFullResponse(String sellerId, String awsRegion, String path,
                                          Map<String, String> queryParams) {
        return executeWithRetry(sellerId, awsRegion, "GET", path, queryParams, null);
    }

    // ── Core execution with retry ─────────────────────────────────────────

    /**
     * Executes the request with retry logic.
     */
    private SpApiResponse executeWithRetry(String sellerId, String awsRegion,
                                            String method, String path,
                                            Map<String, String> queryParams,
                                            Object requestBody) {
        String endpoint = properties.resolveEndpoint(awsRegion);
        int attempt = 0;

        while (true) {
            // Rate limit: wait if the bucket is empty
            rateLimiter.acquire(sellerId, path);

            try {
                SpApiResponse response = executeRequest(
                        sellerId, awsRegion, endpoint, method, path, queryParams, requestBody);

                // Update dynamic rate limits from response header
                String rateLimitHeader = response.getHeaders().get("x-amzn-ratelimit-limit");
                rateLimiter.updateFromResponseHeader(path, rateLimitHeader);

                return response;

            } catch (SpApiException ex) {
                if (!retryPolicy.isRetryable(ex) || !retryPolicy.shouldRetry(attempt)) {
                    log.error("SP-API call failed (non-retryable or max retries): {} {} -> {}",
                            method, path, ex.getMessage());
                    throw ex;
                }

                long retryAfter = -1;
                if (ex instanceof SpApiException.SpApiRateLimitException) {
                    retryAfter = ((SpApiException.SpApiRateLimitException) ex).getRetryAfterSeconds();
                }

                log.warn("SP-API call {} {} failed (attempt {}/{}), retrying: {}",
                        method, path, attempt + 1, retryPolicy.getMaxRetries(), ex.getMessage());

                // On 401, invalidate the cached token so the next attempt gets a fresh one
                if (ex instanceof SpApiException.SpApiAuthException) {
                    tokenStore.invalidateAccessToken(sellerId);
                }

                retryPolicy.sleepForBackoff(attempt, retryAfter);
                attempt++;
            }
        }
    }

    // ── Single request execution ──────────────────────────────────────────

    /**
     * Executes a single HTTP request with full signing and token injection.
     */
    private SpApiResponse executeRequest(String sellerId, String awsRegion, String endpoint,
                                          String method, String path,
                                          Map<String, String> queryParams,
                                          Object requestBody) {
        String accessToken = tokenStore.getAccessToken(sellerId);
        String queryString = buildQueryString(queryParams);
        String bodyString = serializeBody(requestBody);

        // Build canonical request components
        Instant now = Instant.now();
        String amzDate = ISO8601_BASIC.format(now);
        String dateStamp = DATE_ONLY.format(now);

        String urlStr = endpoint + path + (queryString.isEmpty() ? "" : "?" + queryString);
        URI uri = URI.create(urlStr);

        // Compute payload hash
        String payloadHash = sha256Hex(bodyString != null ? bodyString : "");

        // Build and sign the request headers
        Map<String, String> signedHeaders = buildSignedHeaders(
                method, uri, amzDate, dateStamp, awsRegion,
                accessToken, payloadHash, bodyString);

        // Build OkHttp request
        Request.Builder requestBuilder = new Request.Builder().url(urlStr);

        // Add all signed headers
        for (Map.Entry<String, String> entry : signedHeaders.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        // Set the request method and body
        RequestBody okBody = null;
        if (bodyString != null) {
            okBody = RequestBody.create(JSON_MEDIA_TYPE, bodyString);
        }

        switch (method) {
            case "GET":
                requestBuilder.get();
                break;
            case "POST":
                requestBuilder.post(okBody != null ? okBody : RequestBody.create(JSON_MEDIA_TYPE, ""));
                break;
            case "PUT":
                requestBuilder.put(okBody != null ? okBody : RequestBody.create(JSON_MEDIA_TYPE, ""));
                break;
            case "DELETE":
                if (okBody != null) {
                    requestBuilder.delete(okBody);
                } else {
                    requestBuilder.delete();
                }
                break;
            case "PATCH":
                requestBuilder.patch(okBody != null ? okBody : RequestBody.create(JSON_MEDIA_TYPE, ""));
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        log.debug("SP-API {} {} (seller={}, region={})", method, path, sellerId, awsRegion);

        // Execute the request
        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            return parseResponse(response, path);
        } catch (SpApiException e) {
            throw e;
        } catch (IOException e) {
            throw new SpApiException.SpApiClientException(
                    String.format("I/O error calling SP-API [%s %s]: %s", method, path, e.getMessage()),
                    -1, null, null, e);
        }
    }

    // ── AWS Signature V4 ──────────────────────────────────────────────────

    /**
     * Builds the complete set of signed headers for an SP-API request,
     * including the {@code Authorization} header with AWS Signature V4.
     */
    private Map<String, String> buildSignedHeaders(String method, URI uri,
                                                     String amzDate, String dateStamp,
                                                     String awsRegion, String accessToken,
                                                     String payloadHash, String bodyString) {
        TreeMap<String, String> headers = new TreeMap<String, String>();
        headers.put("host", uri.getHost());
        headers.put("x-amz-access-token", accessToken);
        headers.put("x-amz-date", amzDate);
        headers.put("x-amz-content-sha256", payloadHash);
        headers.put("user-agent", "AmazonOpsAI/1.0 (Language=Java/8)");

        // Build canonical headers string (sorted, lowercase keys)
        StringBuilder canonicalHeadersSb = new StringBuilder();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            canonicalHeadersSb.append(e.getKey().toLowerCase())
                    .append(":").append(e.getValue().trim()).append("\n");
        }
        String canonicalHeaders = canonicalHeadersSb.toString() + "\n";

        StringBuilder signedHeadersSb = new StringBuilder();
        boolean first = true;
        for (String key : headers.keySet()) {
            if (!first) {
                signedHeadersSb.append(";");
            }
            signedHeadersSb.append(key.toLowerCase());
            first = false;
        }
        String signedHeadersList = signedHeadersSb.toString();

        // Canonical request
        String canonicalUri = uri.getPath();
        String canonicalQuery = uri.getQuery() != null ? uri.getQuery() : "";
        String canonicalRequest = method + "\n" + canonicalUri + "\n" + canonicalQuery + "\n"
                + canonicalHeaders + "\n" + signedHeadersList + "\n" + payloadHash;

        // String to sign
        String credentialScope = dateStamp + "/" + awsRegion + "/" + SERVICE_NAME + "/aws4_request";
        String stringToSign = AWS4_HMAC_SHA256 + "\n" + amzDate + "\n"
                + credentialScope + "\n" + sha256Hex(canonicalRequest);

        // Signing key derivation
        byte[] signingKey = deriveSigningKey(dateStamp, awsRegion);

        // Signature
        String signature = hmacSha256Hex(signingKey, stringToSign);

        // Authorization header
        String authorization = AWS4_HMAC_SHA256 + " Credential=" + properties.getAwsAccessKeyId()
                + "/" + credentialScope + ", SignedHeaders=" + signedHeadersList
                + ", Signature=" + signature;

        headers.put("authorization", authorization);

        if (bodyString != null) {
            headers.put("content-type", "application/json");
        }

        return headers;
    }

    /**
     * Derives the AWS Signature V4 signing key.
     */
    private byte[] deriveSigningKey(String dateStamp, String region) {
        byte[] kSecret = ("AWS4" + properties.getAwsSecretAccessKey())
                .getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, SERVICE_NAME);
        return hmacSha256(kService, "aws4_request");
    }

    // ── Response handling ─────────────────────────────────────────────────

    /**
     * Parses the OkHttp response into our {@link SpApiResponse},
     * throwing the appropriate exception for error statuses.
     */
    private SpApiResponse parseResponse(Response response, String path) throws IOException {
        int statusCode = response.code();
        String requestId = response.header("x-amzn-RequestId");

        // Extract headers as a simple map (lowercase keys)
        Map<String, String> responseHeaders = new LinkedHashMap<String, String>();
        for (String name : response.headers().names()) {
            responseHeaders.put(name.toLowerCase(), response.header(name));
        }

        ResponseBody responseBody = response.body();
        String body = responseBody != null ? responseBody.string() : "";

        // Error handling: throw appropriate exception for 4xx/5xx
        if (statusCode >= 400) {
            throw SpApiException.fromStatusCode(statusCode, requestId, body, path);
        }

        // Parse JSON body (best-effort)
        JsonNode jsonBody = null;
        if (body != null && !body.trim().isEmpty()) {
            try {
                jsonBody = objectMapper.readTree(body);
            } catch (Exception e) {
                log.debug("Response body is not JSON for {}: {}",
                        path, body.substring(0, Math.min(200, body.length())));
            }
        }

        log.debug("SP-API {} -> {} (requestId={})", path, statusCode, requestId);
        return new SpApiResponse(statusCode, responseHeaders, body, jsonBody, requestId);
    }

    // ── OkHttp client configuration ───────────────────────────────────────

    /**
     * Builds the shared OkHttpClient with timeouts and proxy support.
     */
    private OkHttpClient buildHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // Configure proxy if enabled
        AmazonProperties.Proxy proxyConfig = properties.getProxy();
        if (proxyConfig != null && proxyConfig.isEnabled()) {
            java.net.Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(proxyConfig.getType())
                    ? java.net.Proxy.Type.SOCKS
                    : java.net.Proxy.Type.HTTP;
            builder.proxy(new java.net.Proxy(proxyType,
                    new java.net.InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())));

            String proxyUsername = proxyConfig.getUsername();
            if (proxyUsername != null && !proxyUsername.trim().isEmpty()) {
                builder.proxyAuthenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response resp) throws IOException {
                        String credential = Credentials.basic(
                                proxyConfig.getUsername(), proxyConfig.getPassword());
                        return resp.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    }
                });
            }
        }

        return builder.build();
    }

    // ── Utility methods ───────────────────────────────────────────────────

    /**
     * Builds a URL-encoded query string from parameters.
     */
    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> String.format("%s=%s", urlEncode(e.getKey()), urlEncode(e.getValue())))
                .collect(Collectors.joining("&"));
    }

    /**
     * Serialises a request body to JSON, or returns null for empty bodies.
     */
    private String serializeBody(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String) {
            return (String) body;
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialise request body to JSON", e);
        }
    }

    /** SHA-256 hex digest of a string. */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** HMAC-SHA256 of a string with a key. */
    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 failed", e);
        }
    }

    /** HMAC-SHA256 returning a hex string. */
    private static String hmacSha256Hex(byte[] key, String data) {
        return bytesToHex(hmacSha256(key, data));
    }

    /** Converts a byte array to a lowercase hex string. */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** URL-encodes a string (RFC 3986). */
    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not available", e);
        }
    }
}
