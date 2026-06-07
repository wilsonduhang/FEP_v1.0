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

    /**
     * Returns the SM4 master key used for callback credential ciphertext encryption (16 bytes).
     *
     * <p>Consumed by {@code CredentialEncryptionFacade} (Callback Phase 2b T4) to encrypt
     * outbound interface credentials (TOKEN / OAuth2 client_id+secret) at rest in the
     * {@code callback_credential} table. Each call returns the current active SM4
     * master key; rotation is signaled via {@link #getKeyId()} change.</p>
     *
     * <p><strong>⛔ Mode E:</strong> The real implementation must be written by the
     * security specialist in {@code fep-security-impl}. AI agents must NOT generate
     * the implementation. Key material must come from a HSM, sealed key store, or
     * envelope-encrypted configuration file; never from plaintext on disk. The
     * returned byte array must be 16 bytes (SM4 key length per GB/T 32907-2016)
     * and treated as defensive copy — callers must not retain references beyond
     * a single encrypt/decrypt operation.</p>
     *
     * @return 16-byte SM4 master key (never {@code null})
     */
    byte[] getSm4CredentialMasterKey();

    /**
     * Returns the SM4 master key for a specific key version (16 bytes).
     *
     * <p>Used by {@code CallbackCredentialEncryptionFacade} to decrypt ciphertext that was
     * encrypted under an earlier active key version, enabling multi-version coexistence during
     * key rotation. {@link #getSm4CredentialMasterKey()} (no-arg) returns the current active key
     * used for new encryption; this overload resolves the key recorded on the ciphertext.</p>
     *
     * <p><strong>⛔ Mode E:</strong> The real implementation must be written by the security
     * specialist in {@code fep-security-impl}. AI agents must NOT implement key derivation or
     * HSM/key-store lookup. The returned array must be 16 bytes (SM4 key length per
     * GB/T 32907-2016) and treated as a defensive copy — callers must not retain references
     * beyond a single encrypt/decrypt operation.</p>
     *
     * @param keyId key version identifier (as recorded on the ciphertext); never {@code null}
     * @return 16-byte SM4 master key for that version (never {@code null})
     * @throws IllegalArgumentException if the key version is unknown/unavailable
     */
    byte[] getSm4CredentialMasterKey(String keyId);
}
