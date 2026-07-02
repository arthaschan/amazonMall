package com.yudao.module.amazon.common.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption/decryption for sensitive Amazon credentials.
 *
 * <p>All credential strings (refresh tokens, client secrets, etc.) MUST be
 * encrypted before persistence to MySQL. The ciphertext is self-contained:
 * a 12-byte IV is prepended to the ciphertext, and the entire payload is
 * Base64-encoded for safe storage in VARCHAR columns.
 *
 * <p>Wire format: {@code Base64( IV(12 bytes) || ciphertext || authTag(16 bytes) )}
 *
 * <p>This component is thread-safe; the {@link Cipher} instances are created
 * on every call so there is no shared mutable state.
 *
 * <p>The AES-256-GCM key must be provided via the
 * {@code AMAZON_CREDENTIALS_ENCRYPTION_KEY} environment variable (or the
 * {@code amazon.sp-api.encryption-key} property) as a Base64-encoded 32-byte
 * key.
 *
 * <p>Usage:
 * <pre>{@code
 * String encrypted = encryptor.encrypt("Atza|refresh_token_12345...");
 * // Store `encrypted` in the database
 *
 * // Later...
 * String plainText = encryptor.decrypt(encrypted);
 * assert plainText.equals("Atza|refresh_token_12345...");
 * }</pre>
 */
@Component
public class AmazonCredentialEncryptor {

    private static final Logger log = LoggerFactory.getLogger(AmazonCredentialEncryptor.class);

    /** AES-256-GCM transformation string. */
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    /** GCM nonce length in bytes (12 bytes is recommended for AES-GCM). */
    private static final int GCM_NONCE_LENGTH = 12;

    /** GCM authentication tag length in bits (128 bits = 16 bytes). */
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /** AES-256 key length in bytes. */
    private static final int AES_KEY_LENGTH = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec secretKey;

    /**
     * Creates the encryptor from the configured encryption key.
     *
     * @param properties the SP-API properties containing the Base64-encoded key
     * @throws IllegalStateException if the key is missing or invalid
     */
    public AmazonCredentialEncryptor(AmazonProperties properties) {
        String keyB64 = properties.getEncryptionKey();

        if (keyB64 == null || keyB64.isBlank()) {
            throw new IllegalStateException(
                    """
                    Amazon credential encryption key is not configured.
                    Set AMAZON_CREDENTIALS_ENCRYPTION_KEY (or amazon.sp-api.encryption-key) \
                    to a Base64-encoded 32-byte AES key.
                    Generate one with: openssl rand -base64 32
                    """);
        }

        byte[] raw = Base64.getDecoder().decode(keyB64);
        if (raw.length != AES_KEY_LENGTH) {
            throw new IllegalStateException(
                    "AES-256-GCM key must be exactly %d bytes (got %d)"
                            .formatted(AES_KEY_LENGTH, raw.length));
        }

        this.secretKey = new SecretKeySpec(raw, "AES");
        log.info("Amazon credential encryptor initialised (AES-256-GCM)");
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Encrypts a plaintext credential string.
     *
     * @param plaintext the secret to encrypt (refresh token, client secret, etc.)
     * @return Base64-encoded ciphertext, or {@code null} if input is null/blank
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }

        try {
            // Generate a fresh 12-byte IV for every encryption (critical for GCM security)
            byte[] iv = new byte[GCM_NONCE_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext: [ IV(12) || ciphertext+tag ]
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);

            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt credential", e);
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext produced by {@link #encrypt(String)}.
     *
     * @param encoded the Base64-encoded ciphertext
     * @return the original plaintext, or {@code null} if input is null/blank
     * @throws IllegalStateException if decryption fails (wrong key, tampered data, etc.)
     */
    public String decrypt(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);

            // Extract IV from the first 12 bytes
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_NONCE_LENGTH];
            buffer.get(iv);

            // Remaining bytes are ciphertext + authentication tag
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt credential", e);
        }
    }

    /**
     * Checks whether a given string looks like an encrypted value (Base64-encoded
     * and long enough to contain IV + at least one byte of ciphertext + tag).
     *
     * @param value the string to inspect
     * @return {@code true} if the value appears to be encrypted
     */
    public boolean looksEncrypted(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            // Minimum: 12 (IV) + 1 (ciphertext) + 16 (tag) = 29 bytes
            return decoded.length >= (GCM_NONCE_LENGTH + 1 + 16);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
