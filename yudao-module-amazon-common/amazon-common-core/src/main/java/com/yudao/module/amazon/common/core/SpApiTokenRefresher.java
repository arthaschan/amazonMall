package com.yudao.module.amazon.common.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Handles OAuth2 token exchange and refresh for Amazon SP-API.
 *
 * <p>Amazon SP-API uses the Login with Amazon (LWA) OAuth2 flow:
 * <ol>
 *   <li><strong>Authorization Code Grant</strong>: Exchange a one-time
 *       authorization code (from the OAuth redirect) for an access token
 *       and refresh token. This happens once during seller onboarding.</li>
 *   <li><strong>Refresh Token Grant</strong>: Use the long-lived refresh
 *       token to obtain a new access token (which expires every 60 minutes).</li>
 * </ol>
 *
 * <p>This component is responsible for both flows and handles failures with
 * retry logic. On refresh failure, the caller should alert the operator
 * that re-authorisation is required.
 *
 * @see <a href="https://developer-docs.amazon.com/sp-api/docs/registering-as-a-developer">
 *      Registering as a Developer</a>
 */
@Component
public class SpApiTokenRefresher {

    private static final Logger log = LoggerFactory.getLogger(SpApiTokenRefresher.class);

    /** Form-encoded media type for OAuth2 token requests. */
    private static final MediaType FORM_URL_ENCODED = MediaType.get("application/x-www-form-urlencoded");

    private final AmazonProperties properties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SpApiTokenRefresher(AmazonProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    // ── Response record ───────────────────────────────────────────────────

    /**
     * Token response from the LWA token endpoint.
     */
    public static class TokenResponse {
        /** the short-lived access token (typically 60-minute TTL) */
        private final String accessToken;
        /** the long-lived refresh token (only present in auth-code flow) */
        private final String refreshToken;
        /** access token lifetime in seconds */
        private final int expiresIn;
        /** always "bearer" */
        private final String tokenType;

        public TokenResponse(String accessToken, String refreshToken, int expiresIn, String tokenType) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.tokenType = tokenType;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public int getExpiresIn() { return expiresIn; }
        public String getTokenType() { return tokenType; }

        /** Convenience: when does this access token expire? */
        public Instant expiresAt() {
            return Instant.now().plusSeconds(expiresIn);
        }
    }

    /**
     * Error response from the LWA token endpoint.
     */
    public static class TokenErrorResponse {
        private final String error;
        private final String errorDescription;

        public TokenErrorResponse(String error, String errorDescription) {
            this.error = error;
            this.errorDescription = errorDescription;
        }

        public String getError() { return error; }
        public String getErrorDescription() { return errorDescription; }
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Exchanges a one-time authorization code for access + refresh tokens.
     *
     * <p>Called during seller onboarding when the OAuth redirect callback
     * provides an authorization code.
     *
     * @param authorizationCode the code from the OAuth redirect
     * @return the token response containing access_token and refresh_token
     * @throws SpApiException.SpApiAuthException if the exchange fails
     */
    public TokenResponse exchangeAuthorizationCode(String authorizationCode) {
        log.info("Exchanging authorization code for tokens");

        String formBody = "grant_type=authorization_code"
                + "&code=" + urlEncode(authorizationCode)
                + "&redirect_uri=" + urlEncode(properties.getRedirectUri())
                + "&client_id=" + urlEncode(properties.getClientId())
                + "&client_secret=" + urlEncode(properties.getClientSecret());

        return executeTokenRequest(formBody, "authorization_code");
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * <p>Called when the cached access token has expired or is about to expire.
     * The refresh token is long-lived (typically does not expire unless revoked).
     *
     * @param refreshToken the refresh token for the seller account
     * @return the token response containing a new access_token
     * @throws SpApiException.SpApiAuthException if the refresh fails
     */
    public TokenResponse refreshAccessToken(String refreshToken) {
        log.info("Refreshing access token using refresh token");

        String formBody = "grant_type=refresh_token"
                + "&refresh_token=" + urlEncode(refreshToken)
                + "&client_id=" + urlEncode(properties.getClientId())
                + "&client_secret=" + urlEncode(properties.getClientSecret());

        return executeTokenRequest(formBody, "refresh_token");
    }

    /**
     * Refreshes an access token with retry on transient failures.
     *
     * @param refreshToken the refresh token
     * @param maxRetries   maximum number of retry attempts
     * @return the token response
     * @throws SpApiException.SpApiAuthException if all retry attempts fail
     */
    public TokenResponse refreshAccessTokenWithRetry(String refreshToken, int maxRetries) {
        SpApiException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return refreshAccessToken(refreshToken);
            } catch (SpApiException.SpApiAuthException e) {
                // Auth errors are not retryable (invalid refresh token)
                throw e;
            } catch (Exception e) {
                lastException = new SpApiException.SpApiAuthException(
                        "Token refresh failed on attempt " + attempt, e);

                if (attempt < maxRetries) {
                    long backoff = properties.getInitialBackoffMs() * (1L << attempt);
                    backoff = Math.min(backoff, properties.getMaxBackoffMs());
                    log.warn("Token refresh failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt + 1, maxRetries + 1, backoff, e.getMessage());

                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SpApiException.SpApiAuthException(
                                "Token refresh retry interrupted", ie);
                    }
                }
            }
        }

        throw lastException != null ? lastException
                : new SpApiException.SpApiAuthException("Token refresh failed", null);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    /**
     * Executes the token request against the LWA endpoint and parses the response.
     */
    private TokenResponse executeTokenRequest(String formBody, String grantType) {
        Request request = new Request.Builder()
                .url(properties.getTokenEndpoint())
                .post(RequestBody.create(formBody, FORM_URL_ENCODED))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody != null ? responseBody.string() : "";

            if (!response.isSuccessful()) {
                // Parse the error response
                try {
                    TokenErrorResponse error = objectMapper.readValue(body, TokenErrorResponse.class);
                    log.error("LWA token error [{}]: {} - {}", grantType, error.getError(), error.getErrorDescription());

                    if ("invalid_grant".equals(error.getError())) {
                        throw new SpApiException.SpApiAuthException(
                                String.format("Invalid grant: %s", error.getErrorDescription()), null);
                    }
                    throw new SpApiException.SpApiAuthException(
                            String.format("Token request failed: %s - %s", error.getError(), error.getErrorDescription()),
                            null);
                } catch (SpApiException.SpApiAuthException e) {
                    throw e;
                } catch (Exception parseError) {
                    throw new SpApiException.SpApiAuthException(
                            String.format("Token request failed with status %d: %s",
                                    response.code(), body), null);
                }
            }

            // Parse successful response
            // The LWA response uses snake_case: access_token, refresh_token, expires_in, token_type
            JsonNode jsonNode = objectMapper.readTree(body);
            String accessToken = jsonNode.path("access_token").asText(null);
            String newRefreshToken = jsonNode.has("refresh_token")
                    ? jsonNode.path("refresh_token").asText(null) : null;
            int expiresIn = jsonNode.path("expires_in").asInt(3600);
            String tokenType = jsonNode.path("token_type").asText("bearer");

            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new SpApiException.SpApiAuthException(
                        "Empty access_token in LWA response", null);
            }

            log.info("Token {} successful - access token expires in {}s", grantType, expiresIn);
            return new TokenResponse(accessToken, newRefreshToken, expiresIn, tokenType);

        } catch (SpApiException.SpApiAuthException e) {
            throw e;
        } catch (IOException e) {
            throw new SpApiException.SpApiAuthException(
                    "Failed to communicate with LWA token endpoint: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new SpApiException.SpApiAuthException(
                    "Unexpected error during token " + grantType + ": " + e.getMessage(), e);
        }
    }

    /**
     * URL-encodes a string for use in form bodies.
     */
    private static String urlEncode(String value) {
        if (value == null) return "";
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
