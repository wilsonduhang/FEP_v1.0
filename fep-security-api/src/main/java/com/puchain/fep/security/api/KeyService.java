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

    /**
     * Returns the SM2 private key used for outbound message signing (PKCS#8 encoded).
     *
     * <p>Consumed by {@code OutboundSignAdapter} (P5 T5) to compute the SM3withSM2
     * signature that is embedded as an XML comment immediately before {@code </CFX>}.</p>
     *
     * <p><strong>⛔ Mode E:</strong> The real implementation must be written by the
     * security specialist in {@code fep-security-impl}. AI agents must NOT generate
     * the implementation. Key material must come from a HSM or sealed key store;
     * never from plaintext on disk.</p>
     *
     * @return PKCS#8-encoded SM2 private key bytes (never {@code null})
     */
    byte[] getSignPrivateKey();
}
