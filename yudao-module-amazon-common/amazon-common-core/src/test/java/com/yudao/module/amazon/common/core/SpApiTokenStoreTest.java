package com.yudao.module.amazon.common.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SP-API token management covering caching, refresh flows,
 * expiration handling, concurrency safety, and encryption.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SP-API Token Store Tests")
class SpApiTokenStoreTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SpApiTokenRepository tokenRepository;

    @Mock
    private SpApiCredentialEncryptor encryptor;

    @Mock
    private AmazonLwaClient lwaClient;

    private SpApiTokenStore tokenStore;

    private static final String MARKETPLACE_US = "ATVPDKIKX0DER";
    private static final String SELLER_ID = "A1234567890";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(60);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenStore = new SpApiTokenStore(redisTemplate, tokenRepository, encryptor, lwaClient);
    }

    // -----------------------------------------------------------------------
    // Token Caching
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Token is stored in Redis with correct TTL after refresh")
    void testTokenCaching() {
        // Given: A successful token refresh
        String accessToken = "Atza|new-access-token-xyz";
        LwaTokenResponse tokenResponse = new LwaTokenResponse(
                accessToken,
                "Atzr|new-refresh-token",
                3600 // expires_in seconds
        );

        when(lwaClient.refreshToken(any(), any(), any())).thenReturn(tokenResponse);
        when(encryptor.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptor.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // When: Requesting an access token
        String result = tokenStore.getAccessToken(MARKETPLACE_US);

        // Then: Token is cached in Redis with proper TTL
        verify(valueOperations).set(
                eq("spapi:token:" + MARKETPLACE_US),
                eq(accessToken),
                eq(Duration.ofSeconds(3500)) // slightly less than 3600 for safety margin
        );

        assertThat(result).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("Subsequent requests return cached token without refresh")
    void testCachedTokenRetrieval() {
        // Given: Token already cached in Redis
        String cachedToken = "Atza|cached-token";
        when(valueOperations.get("spapi:token:" + MARKETPLACE_US)).thenReturn(cachedToken);

        // When: Requesting access token
        String result = tokenStore.getAccessToken(MARKETPLACE_US);

        // Then: Cached token is returned, no refresh triggered
        assertThat(result).isEqualTo(cachedToken);
        verify(lwaClient, never()).refreshToken(any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // Token Refresh
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Refresh token flow updates both access and refresh tokens")
    void testTokenRefresh() {
        // Given: Expired token requiring refresh
        when(valueOperations.get("spapi:token:" + MARKETPLACE_US)).thenReturn(null);

        String newAccessToken = "Atza|fresh-access-token";
        String newRefreshToken = "Atzr|fresh-refresh-token";
        LwaTokenResponse tokenResponse = new LwaTokenResponse(newAccessToken, newRefreshToken, 3600);

        when(lwaClient.refreshToken(any(), any(), any())).thenReturn(tokenResponse);
        when(encryptor.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptor.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // When: Explicitly refreshing the token
        tokenStore.refreshAccessToken(MARKETPLACE_US);

        // Then: New access token is cached
        verify(valueOperations).set(
                eq("spapi:token:" + MARKETPLACE_US),
                eq(newAccessToken),
                any(Duration.class)
        );

        // And: New refresh token is persisted to database (encrypted)
        verify(tokenRepository).updateRefreshToken(eq(MARKETPLACE_US), anyString());
        verify(encryptor).encrypt(newRefreshToken);
    }

    // -----------------------------------------------------------------------
    // Token Expiration
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Expired token in cache triggers automatic refresh")
    void testTokenExpiration() {
        // Given: Cache returns null (token expired/evicted)
        when(valueOperations.get("spapi:token:" + MARKETPLACE_US))
                .thenReturn(null);

        String freshToken = "Atza|auto-refreshed-token";
        LwaTokenResponse tokenResponse = new LwaTokenResponse(freshToken, "Atzr|new-refresh", 3600);
        when(lwaClient.refreshToken(any(), any(), any())).thenReturn(tokenResponse);
        when(encryptor.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptor.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // When: Requesting token after expiration
        String result = tokenStore.getAccessToken(MARKETPLACE_US);

        // Then: Token was automatically refreshed
        assertThat(result).isEqualTo(freshToken);
        verify(lwaClient).refreshToken(any(), any(), any());
    }

    @Test
    @DisplayName("Token with remaining TTL less than 5 minutes triggers proactive refresh")
    void testProactiveTokenRefresh() {
        // Given: Token exists but is about to expire (TTL < 5 min)
        String expiringToken = "Atza|about-to-expire";
        when(valueOperations.get("spapi:token:" + MARKETPLACE_US)).thenReturn(expiringToken);
        when(redisTemplate.getExpire("spapi:token:" + MARKETPLACE_US, TimeUnit.SECONDS))
                .thenReturn(180L); // 3 minutes remaining

        String freshToken = "Atza|proactively-refreshed";
        LwaTokenResponse tokenResponse = new LwaTokenResponse(freshToken, "Atzr|refresh", 3600);
        when(lwaClient.refreshToken(any(), any(), any())).thenReturn(tokenResponse);
        when(encryptor.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptor.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // When: Requesting token with low remaining TTL
        String result = tokenStore.getAccessToken(MARKETPLACE_US);

        // Then: Token was proactively refreshed
        assertThat(result).isEqualTo(freshToken);
        verify(lwaClient).refreshToken(any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // Concurrent Refresh
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Multiple threads requesting refresh only trigger one actual refresh")
    void testConcurrentRefresh() throws InterruptedException {
        // Given: No cached token, multiple threads requesting simultaneously
        when(valueOperations.get("spapi:token:" + MARKETPLACE_US)).thenReturn(null);

        AtomicInteger refreshCount = new AtomicInteger(0);
        when(lwaClient.refreshToken(any(), any(), any())).thenAnswer(inv -> {
            refreshCount.incrementAndGet();
            Thread.sleep(100); // Simulate network delay
            return new LwaTokenResponse("Atza|concurrent-token", "Atzr|refresh", 3600);
        });

        when(encryptor.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptor.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // When: 10 threads request token simultaneously
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    tokenStore.getAccessToken(MARKETPLACE_US);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: Only one refresh was performed (distributed lock prevented duplicates)
        assertThat(refreshCount.get())
                .as("Only one refresh should occur despite concurrent requests")
                .isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Refresh Failure
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Refresh failure triggers retry with exponential backoff")
    void testRefreshFailure() {
        // Given: LWA service temporarily unavailable
        when(valueOperations.get("spapi:token:" + MARKETPLACE_US)).thenReturn(null);

        when(lwaClient.refreshToken(any(), any(), any()))
                .thenThrow(new LwaServiceException("Service temporarily unavailable"))
                .thenThrow(new LwaServiceException("Service temporarily unavailable"))
                .thenReturn(new LwaTokenResponse("Atza|recovered-token", "Atzr|refresh", 3600));

        when(encryptor.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(encryptor.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // When: Requesting token with retry logic
        String result = tokenStore.getAccessToken(MARKETPLACE_US);

        // Then: Eventually succeeds after retries
        assertThat(result).isEqualTo("Atza|recovered-token");
        verify(lwaClient, times(3)).refreshToken(any(), any(), any());
    }

    @Test
    @DisplayName("Persistent refresh failure throws exception after max retries")
    void testPersistentRefreshFailure() {
        // Given: LWA service completely down
        when(valueOperations.get("spapi:token:" + MARKETPLACE_US)).thenReturn(null);
        when(lwaClient.refreshToken(any(), any(), any()))
                .thenThrow(new LwaServiceException("Service permanently unavailable"));

        when(encryptor.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        // When/Then: Exception is thrown after exhausting retries
        assertThatThrownBy(() -> tokenStore.getAccessToken(MARKETPLACE_US))
                .isInstanceOf(SpApiTokenRefreshException.class)
                .hasMessageContaining("Failed to refresh token after")
                .hasCauseInstanceOf(LwaServiceException.class);

        // And: Retried maximum number of times (e.g., 3)
        verify(lwaClient, times(3)).refreshToken(any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // Token Encryption
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Tokens are encrypted in MySQL and decrypted on read")
    void testTokenEncryption() {
        // Given: Encrypted tokens in database
        String encryptedAccessToken = "ENC:atza:encrypted-access";
        String encryptedRefreshToken = "ENC:atzr:encrypted-refresh";

        SpApiTokenEntity entity = new SpApiTokenEntity();
        entity.setMarketplaceId(MARKETPLACE_US);
        entity.setAccessTokenEncrypted(encryptedAccessToken);
        entity.setRefreshTokenEncrypted(encryptedRefreshToken);
        entity.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));

        when(tokenRepository.findByMarketplaceId(MARKETPLACE_US)).thenReturn(entity);
        when(encryptor.decrypt(encryptedAccessToken)).thenReturn("Atza|decrypted-access");
        when(encryptor.decrypt(encryptedRefreshToken)).thenReturn("Atzr|decrypted-refresh");

        // When: Loading token from database
        SpApiToken token = tokenStore.loadFromDatabase(MARKETPLACE_US);

        // Then: Token is properly decrypted
        assertThat(token.getAccessToken()).isEqualTo("Atza|decrypted-access");
        assertThat(token.getRefreshToken()).isEqualTo("Atzr|decrypted-refresh");
        verify(encryptor).decrypt(encryptedAccessToken);
        verify(encryptor).decrypt(encryptedRefreshToken);
    }

    @Test
    @DisplayName("New tokens are encrypted before persisting to database")
    void testTokenEncryptionOnSave() {
        // Given: Fresh token from LWA
        String plainAccessToken = "Atza|plain-access-token";
        String plainRefreshToken = "Atzr|plain-refresh-token";

        when(encryptor.encrypt(plainAccessToken)).thenReturn("ENC:atza:xyz123");
        when(encryptor.encrypt(plainRefreshToken)).thenReturn("ENC:atzr:abc456");

        // When: Saving token to database
        tokenStore.saveToDatabase(MARKETPLACE_US, plainAccessToken, plainRefreshToken, Instant.now().plus(Duration.ofHours(1)));

        // Then: Tokens are encrypted before storage
        verify(encryptor).encrypt(plainAccessToken);
        verify(encryptor).encrypt(plainRefreshToken);
        verify(tokenRepository).save(argThat(entity -> {
            assertThat(entity.getAccessTokenEncrypted()).startsWith("ENC:");
            assertThat(entity.getRefreshTokenEncrypted()).startsWith("ENC:");
            return true;
        }));
    }

    @Test
    @DisplayName("Encryption key rotation re-encrypts all stored tokens")
    void testEncryptionKeyRotation() {
        // Given: Multiple tokens stored with old encryption key
        SpApiTokenEntity entity1 = new SpApiTokenEntity();
        entity1.setMarketplaceId("US");
        entity1.setAccessTokenEncrypted("OLD_ENC:us-access");
        entity1.setRefreshTokenEncrypted("OLD_ENC:us-refresh");

        SpApiTokenEntity entity2 = new SpApiTokenEntity();
        entity2.setMarketplaceId("UK");
        entity2.setAccessTokenEncrypted("OLD_ENC:uk-access");
        entity2.setRefreshTokenEncrypted("OLD_ENC:uk-refresh");

        when(tokenRepository.findAll()).thenReturn(java.util.List.of(entity1, entity2));
        when(encryptor.decrypt("OLD_ENC:us-access")).thenReturn("Atza|us-access");
        when(encryptor.decrypt("OLD_ENC:us-refresh")).thenReturn("Atzr|us-refresh");
        when(encryptor.decrypt("OLD_ENC:uk-access")).thenReturn("Atza|uk-access");
        when(encryptor.decrypt("OLD_ENC:uk-refresh")).thenReturn("Atzr|uk-refresh");

        when(encryptor.encrypt("Atza|us-access")).thenReturn("NEW_ENC:us-access");
        when(encryptor.encrypt("Atzr|us-refresh")).thenReturn("NEW_ENC:us-refresh");
        when(encryptor.encrypt("Atza|uk-access")).thenReturn("NEW_ENC:uk-access");
        when(encryptor.encrypt("Atzr|uk-refresh")).thenReturn("NEW_ENC:uk-refresh");

        // When: Rotating encryption keys
        tokenStore.rotateEncryptionKey();

        // Then: All tokens are re-encrypted with new key
        verify(tokenRepository, times(2)).save(argThat(entity ->
                entity.getAccessTokenEncrypted().startsWith("NEW_ENC:")
        ));
    }
}
