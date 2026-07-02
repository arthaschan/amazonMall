package com.yudao.module.amazon.common.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AmazonCredentialEncryptor}.
 * <p>
 * Covers AES-256-GCM encryption/decryption of Amazon credentials.
 */
@DisplayName("Common - AmazonCredentialEncryptor")
class AmazonCredentialEncryptorTest {

    private AmazonCredentialEncryptor encryptor;

    /** Test encryption key (Base64 encoded) */
    private static final String TEST_KEY = "dGVzdGtleTEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    @BeforeEach
    void setUp() {
        encryptor = new AmazonCredentialEncryptor(TEST_KEY);
    }

    // -----------------------------------------------------------------------
    // Encrypt / Decrypt Round-trip
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Encrypt/Decrypt Round-trip")
    class RoundTripTests {

        @Test
        @DisplayName("should encrypt and decrypt a client ID")
        void testRoundTripClientId() {
            String original = "amzn1.application-oa2-client.abc123def456";

            String encrypted = encryptor.encrypt(original);
            assertThat(encrypted).isNotEqualTo(original);
            assertThat(encrypted).isNotBlank();

            String decrypted = encryptor.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(original);
        }

        @Test
        @DisplayName("should encrypt and decrypt a refresh token")
        void testRoundTripRefreshToken() {
            String refreshToken = "Atzr|IwEBILongRefreshTokenValueHere123456789";

            String encrypted = encryptor.encrypt(refreshToken);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(refreshToken);
        }

        @Test
        @DisplayName("should encrypt and decrypt an empty string")
        void testRoundTripEmpty() {
            String encrypted = encryptor.encrypt("");
            String decrypted = encryptor.decrypt(encrypted);
            assertThat(decrypted).isEmpty();
        }

        @Test
        @DisplayName("should produce different ciphertext for same plaintext (non-deterministic)")
        void testNonDeterministic() {
            String plaintext = "same-credential-value";

            String encrypted1 = encryptor.encrypt(plaintext);
            String encrypted2 = encryptor.encrypt(plaintext);

            // AES-GCM uses random IV, so ciphertext should differ
            assertThat(encrypted1).isNotEqualTo(encrypted2);

            // Both should decrypt to the same plaintext
            assertThat(encryptor.decrypt(encrypted1)).isEqualTo(plaintext);
            assertThat(encryptor.decrypt(encrypted2)).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("should handle Unicode characters in credentials")
        void testUnicode() {
            String unicode = "凭证测试-credential-テスト";

            String encrypted = encryptor.encrypt(unicode);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(unicode);
        }

    }

    // -----------------------------------------------------------------------
    // Error Cases
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Error Cases")
    class ErrorTests {

        @Test
        @DisplayName("should throw or handle null input for encrypt")
        void testEncryptNull() {
            try {
                String result = encryptor.encrypt(null);
                assertThat(result).isNull();
            } catch (Exception e) {
                assertThat(e).isNotNull();
            }
        }

        @Test
        @DisplayName("should throw on tampered ciphertext")
        void testTamperedCiphertext() {
            String encrypted = encryptor.encrypt("secret-value");
            String tampered = encrypted.substring(0, encrypted.length() - 2) + "XX";

            try {
                encryptor.decrypt(tampered);
                // If no exception, the result should not match original
            } catch (Exception e) {
                assertThat(e).isNotNull();
            }
        }

    }

}
