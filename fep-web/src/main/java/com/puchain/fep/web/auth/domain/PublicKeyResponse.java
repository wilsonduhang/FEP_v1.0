package com.puchain.fep.web.auth.domain;

/**
 * Response DTO for GET /api/v1/auth/public-key.
 *
 * <p>Contains the current SM2 public key for client-side password encryption,
 * along with the key version identifier and algorithm name.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class PublicKeyResponse {

    private final String publicKeyBase64;
    private final String keyId;
    private final String algorithm;

    /**
     * Constructs a PublicKeyResponse.
     *
     * @param publicKeyBase64 Base64-encoded SM2 public key
     * @param keyId           key version identifier
     * @param algorithm       algorithm name (e.g. "SM2")
     */
    public PublicKeyResponse(final String publicKeyBase64, final String keyId, final String algorithm) {
        this.publicKeyBase64 = publicKeyBase64;
        this.keyId = keyId;
        this.algorithm = algorithm;
    }

    /**
     * Returns the Base64-encoded SM2 public key.
     *
     * @return public key in Base64
     */
    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    /**
     * Returns the key version identifier.
     *
     * @return key ID string
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Returns the algorithm name.
     *
     * @return algorithm identifier (e.g. "SM2")
     */
    public String getAlgorithm() {
        return algorithm;
    }
}
