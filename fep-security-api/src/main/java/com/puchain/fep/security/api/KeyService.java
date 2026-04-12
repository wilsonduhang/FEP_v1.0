package com.puchain.fep.security.api;

/**
 * SM2 key management service.
 *
 * <p>Provides public key distribution for client-side password encryption
 * and server-side decryption during login.</p>
 *
 * <p><strong>Security note:</strong> The real implementation in {@code security-impl}
 * must be written by the security specialist (Mode E). This interface and its
 * mock are AI-generated (Mode B).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface KeyService {

    /**
     * Returns the current SM2 public key in Base64 encoding.
     *
     * @return Base64-encoded SM2 public key (X.509 SubjectPublicKeyInfo format)
     */
    String getSm2PublicKeyBase64();

    /**
     * Returns the key ID (version) of the current active SM2 key pair.
     *
     * @return key identifier string
     */
    String getKeyId();

    /**
     * Decrypts a login password that was encrypted with the SM2 public key.
     *
     * @param encryptedBase64 Base64-encoded SM2 ciphertext
     * @param keyId           key version used for encryption (for key rotation support)
     * @return cleartext password
     * @throws IllegalArgumentException if decryption fails
     */
    String decryptLoginPassword(String encryptedBase64, String keyId);
}
