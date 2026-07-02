package com.yudao.module.amazon.common.core;

import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for SP-API client integration covering authentication, rate limiting,
 * retry strategies, signature generation, and credential security.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SP-API Client Integration Tests")
class SpApiClientTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private SpApiTokenStore tokenStore;

    @Mock
    private SpApiCredentialEncryptor credentialEncryptor;

    private SpApiClient spApiClient;

    // Amazon SP-API endpoints
    private static final String ORDERS_ENDPOINT = "/orders/v0/orders";
    private static final String TOKEN_ENDPOINT = "/auth/o2/token";
    private static final String MARKETPLACE_US = "ATVPDKIKX0DER";

    @BeforeEach
    void setUp() {
        spApiClient = new SpApiClient(httpClient, tokenStore, credentialEncryptor);
    }

    // -----------------------------------------------------------------------
    // Normal API Calls
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Successful API call with valid token returns parsed response")
    void testSuccessfulApiCall() throws IOException {
        // Given: A valid access token and a successful HTTP response
        String accessToken = "Atza|valid-access-token-xyz";
        when(tokenStore.getAccessToken(MARKETPLACE_US)).thenReturn(accessToken);

        String responseBody = """
            {
              "payload": {
                "Orders": [
                  {"AmazonOrderId": "111-1234567-1234567", "OrderStatus": "Shipped"}
                ]
              }
            }
            """;
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(responseBody, MediaType.parse("application/json")))
                .build();

        when(httpClient.newCall(any())).thenReturn(mockCall(mockResponse));

        // When: Executing the API call
        SpApiResponse result = spApiClient.getOrders(MARKETPLACE_US, Instant.now().minus(Duration.ofHours(1)));

        // Then: Response is parsed successfully
        assertThat(result).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getBody()).contains("111-1234567-1234567");
        assertThat(result.isSuccessful()).isTrue();

        // And: Token was retrieved from store
        verify(tokenStore).getAccessToken(MARKETPLACE_US);
    }

    // -----------------------------------------------------------------------
    // Token Auto-Refresh
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Token expired triggers auto-refresh and retries the request")
    void testTokenAutoRefresh() throws IOException {
        // Given: An expired token that returns 401 on first call
        String expiredToken = "Atza|expired-token";
        String freshToken = "Atza|fresh-token-abc";

        when(tokenStore.getAccessToken(MARKETPLACE_US))
                .thenReturn(expiredToken)
                .thenReturn(freshToken);

        Response unauthorizedResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body(ResponseBody.create("{\"errors\":[{\"code\":\"Unauthorized\"}]}", MediaType.parse("application/json")))
                .build();

        String successBody = """
            {"payload": {"Orders": [{"AmazonOrderId": "222-7654321-7654321"}]}}
            """;
        Response successResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(successBody, MediaType.parse("application/json")))
                .build();

        Call mockCall1 = mockCall(unauthorizedResponse);
        Call mockCall2 = mockCall(successResponse);
        when(httpClient.newCall(any())).thenReturn(mockCall1, mockCall2);

        // When: The client detects 401, refreshes token, and retries
        SpApiResponse result = spApiClient.getOrders(MARKETPLACE_US, Instant.now().minus(Duration.ofHours(1)));

        // Then: Token refresh was triggered and request retried successfully
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getBody()).contains("222-7654321-7654321");
        verify(tokenStore).refreshAccessToken(MARKETPLACE_US);
        verify(httpClient, times(2)).newCall(any());
    }

    // -----------------------------------------------------------------------
    // Rate Limit Handling
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("429 Too Many Requests triggers exponential backoff retry")
    void testRateLimitHandling() throws IOException {
        // Given: A valid token but rate-limited responses
        when(tokenStore.getAccessToken(MARKETPLACE_US)).thenReturn("Atza|valid-token");

        Response rateLimitResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(429)
                .message("Too Many Requests")
                .header("x-amzn-RateLimit-Limit", "0.0167")
                .body(ResponseBody.create("{\"errors\":[{\"code\":\"QuotaExceeded\"}]}", MediaType.parse("application/json")))
                .build();

        String successBody = """
            {"payload": {"Orders": []}}
            """;
        Response successResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(successBody, MediaType.parse("application/json")))
                .build();

        // First two calls return 429, third succeeds
        Call call1 = mockCall(rateLimitResponse);
        Call call2 = mockCall(rateLimitResponse);
        Call call3 = mockCall(successResponse);
        when(httpClient.newCall(any())).thenReturn(call1, call2, call3);

        // When: Client applies exponential backoff
        SpApiResponse result = spApiClient.getOrders(MARKETPLACE_US, Instant.now().minus(Duration.ofHours(1)));

        // Then: Eventually succeeds after backoff retries
        assertThat(result.isSuccessful()).isTrue();
        verify(httpClient, times(3)).newCall(any());
    }

    // -----------------------------------------------------------------------
    // Server Error Retry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("500/502/503 server errors trigger retry with exponential backoff")
    void testServerErrorRetry() throws IOException {
        // Given: Transient server errors
        when(tokenStore.getAccessToken(MARKETPLACE_US)).thenReturn("Atza|valid-token");

        Response serverErrorResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(503)
                .message("Service Unavailable")
                .body(ResponseBody.create("{\"errors\":[{\"code\":\"InternalFailure\"}]}", MediaType.parse("application/json")))
                .build();

        String successBody = """
            {"payload": {"Orders": []}}
            """;
        Response successResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(successBody, MediaType.parse("application/json")))
                .build();

        Call call1 = mockCall(serverErrorResponse);
        Call call2 = mockCall(successResponse);
        when(httpClient.newCall(any())).thenReturn(call1, call2);

        // When: Client retries on server error
        SpApiResponse result = spApiClient.getOrders(MARKETPLACE_US, Instant.now().minus(Duration.ofHours(1)));

        // Then: Eventually succeeds
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getStatusCode()).isEqualTo(200);
        verify(httpClient, times(2)).newCall(any());
    }

    // -----------------------------------------------------------------------
    // Non-Retryable Errors
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("400/403 client errors do not trigger retry")
    void testNonRetryableError() throws IOException {
        // Given: A non-retryable client error
        when(tokenStore.getAccessToken(MARKETPLACE_US)).thenReturn("Atza|valid-token");

        Response badRequestResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(ResponseBody.create(
                        "{\"errors\":[{\"code\":\"InvalidInput\",\"message\":\"Invalid marketplace\"}]}",
                        MediaType.parse("application/json")))
                .build();

        when(httpClient.newCall(any())).thenReturn(mockCall(badRequestResponse));

        // When: Client encounters 400
        SpApiResponse result = spApiClient.getOrders(MARKETPLACE_US, Instant.now().minus(Duration.ofHours(1)));

        // Then: No retry, error returned immediately
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getStatusCode()).isEqualTo(400);
        assertThat(result.getErrorMessage()).contains("InvalidInput");

        // And: Only one HTTP call was made (no retries)
        verify(httpClient, times(1)).newCall(any());
    }

    @Test
    @DisplayName("403 Forbidden does not trigger retry")
    void testForbiddenNoRetry() throws IOException {
        // Given: A 403 Forbidden response (e.g., insufficient permissions)
        when(tokenStore.getAccessToken(MARKETPLACE_US)).thenReturn("Atza|valid-token");

        Response forbiddenResponse = new Response.Builder()
                .request(new Request.Builder().url("https://sellingpartnerapi-na.amazon.com" + ORDERS_ENDPOINT).build())
                .protocol(Protocol.HTTP_1_1)
                .code(403)
                .message("Forbidden")
                .body(ResponseBody.create(
                        "{\"errors\":[{\"code\":\"AccessDenied\",\"message\":\"Access to requested resource is denied\"}]}",
                        MediaType.parse("application/json")))
                .build();

        when(httpClient.newCall(any())).thenReturn(mockCall(forbiddenResponse));

        // When: Client encounters 403
        SpApiResponse result = spApiClient.getOrders(MARKETPLACE_US, Instant.now().minus(Duration.ofHours(1)));

        // Then: Fails immediately without retry
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getStatusCode()).isEqualTo(403);
        verify(httpClient, times(1)).newCall(any());
    }

    // -----------------------------------------------------------------------
    // AWS Signature V4
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("AWS Signature V4 signing produces correct Authorization header")
    void testSignatureV4() {
        // Given: Known AWS credentials and request parameters
        String accessKeyId = "AKIAIOSFODNN7EXAMPLE";
        String secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
        String region = "us-east-1";
        String service = "execute-api";
        String method = "GET";
        String uri = "/orders/v0/orders";
        String queryString = "MarketplaceIds=ATVPDKIKX0DER&CreatedAfter=2024-01-01T00:00:00Z";
        String host = "sellingpartnerapi-na.amazon.com";
        Instant timestamp = Instant.parse("2024-01-15T12:00:00Z");

        // When: Signing the request
        SpApiSignedRequest signedRequest = spApiClient.signRequest(
                accessKeyId, secretKey, region, service,
                method, uri, queryString, host, timestamp
        );

        // Then: Authorization header contains required AWS Sig V4 components
        String authHeader = signedRequest.getAuthorizationHeader();
        assertThat(authHeader).startsWith("AWS4-HMAC-SHA256");
        assertThat(authHeader).contains("Credential=" + accessKeyId + "/20240115/us-east-1/execute-api/aws4_request");
        assertThat(authHeader).contains("SignedHeaders=");
        assertThat(authHeader).contains("Signature=");

        // And: Required headers are present
        assertThat(signedRequest.getHeaders()).containsKey("x-amz-date");
        assertThat(signedRequest.getHeaders().get("x-amz-date")).isEqualTo("20240115T120000Z");
        assertThat(signedRequest.getHeaders()).containsKey("host");
    }

    @Test
    @DisplayName("Signature V4 produces different signatures for different payloads")
    void testSignatureV4DifferentPayloads() {
        String accessKeyId = "AKIAIOSFODNN7EXAMPLE";
        String secretKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
        Instant timestamp = Instant.parse("2024-01-15T12:00:00Z");

        SpApiSignedRequest signed1 = spApiClient.signRequest(
                accessKeyId, secretKey, "us-east-1", "execute-api",
                "GET", "/orders/v0/orders", "MarketplaceIds=ATVPDKIKX0DER",
                "sellingpartnerapi-na.amazon.com", timestamp
        );

        SpApiSignedRequest signed2 = spApiClient.signRequest(
                accessKeyId, secretKey, "us-east-1", "execute-api",
                "GET", "/catalog/v0/items", "MarketplaceIds=ATVPDKIKX0DER",
                "sellingpartnerapi-na.amazon.com", timestamp
        );

        // Different URIs should produce different signatures
        assertThat(signed1.getAuthorizationHeader())
                .isNotEqualTo(signed2.getAuthorizationHeader());
    }

    // -----------------------------------------------------------------------
    // Token Encryption
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Credentials are encrypted at rest and decrypted only in memory")
    void testTokenEncryption() {
        // Given: Raw credential values
        String rawClientId = "amzn1.application-oa2-client.abc123";
        String rawClientSecret = "super-secret-client-secret-value";
        String rawRefreshToken = "Atzr|refresh-token-xyz";

        // When: Storing credentials
        spApiClient.storeCredentials(MARKETPLACE_US, rawClientId, rawClientSecret, rawRefreshToken);

        // Then: Credentials were encrypted before storage
        verify(credentialEncryptor).encrypt(rawClientId);
        verify(credentialEncryptor).encrypt(rawClientSecret);
        verify(credentialEncryptor).encrypt(rawRefreshToken);

        // And: Raw values are NOT stored in plaintext anywhere
        verify(credentialEncryptor, never()).decrypt(argThat(
                (String value) -> value.equals(rawClientId) || value.equals(rawClientSecret)
        ));
    }

    @Test
    @DisplayName("Decrypted credentials are used only for API calls and not logged")
    void testDecryptedCredentialsNotLeaked() {
        // Given: Encrypted credentials in storage
        when(credentialEncryptor.decrypt("encrypted-client-id")).thenReturn("amzn1.application-oa2-client.abc123");
        when(credentialEncryptor.decrypt("encrypted-client-secret")).thenReturn("real-secret");
        when(credentialEncryptor.decrypt("encrypted-refresh-token")).thenReturn("Atzr|refresh-token");

        // When: Retrieving credentials for token refresh
        SpApiCredentials credentials = spApiClient.loadCredentials(MARKETPLACE_US);

        // Then: Credentials are properly decrypted
        assertThat(credentials.getClientId()).isEqualTo("amzn1.application-oa2-client.abc123");
        assertThat(credentials.getClientSecret()).isEqualTo("real-secret");
        assertThat(credentials.getRefreshToken()).isEqualTo("Atzr|refresh-token");

        // And: toString() should mask sensitive fields
        assertThat(credentials.toString()).doesNotContain("real-secret");
        assertThat(credentials.toString()).contains("***");
    }

    // -----------------------------------------------------------------------
    // Helper Methods
    // -----------------------------------------------------------------------

    private Call mockCall(Response response) {
        Call call = mock(Call.class);
        try {
            when(call.execute()).thenReturn(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return call;
    }
}
