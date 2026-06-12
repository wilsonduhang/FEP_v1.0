package com.puchain.fep.security.impl.audit;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.security.api.HashService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.SignService;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 审计完整性原语实现（组合 HashService SM3 + SignService SM2 + KeyService 审计密钥）。
 *
 * <p>无 Spring stereotype，经 GmAuditConfiguration @Bean 注册（always-on——hash 链恒真 SM3；
 * 签名强度随 provider：mock 域占位签名、impl 域真 SM2，镜像登录 mock 哲学）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class AuditIntegrityServiceImpl implements AuditIntegrityService {

    private final HashService hashService;
    private final SignService signService;
    private final KeyService keyService;

    /**
     * 组合构造。
     *
     * @param hashService SM3 摘要，非 null
     * @param signService SM2 签名，非 null
     * @param keyService  审计密钥访问，非 null
     */
    public AuditIntegrityServiceImpl(final HashService hashService,
            final SignService signService, final KeyService keyService) {
        this.hashService = Objects.requireNonNull(hashService, "hashService");
        this.signService = Objects.requireNonNull(signService, "signService");
        this.keyService = Objects.requireNonNull(keyService, "keyService");
    }

    @Override
    public String computeEntryHash(final String prevHashHex, final byte[] canonical) {
        if (prevHashHex == null || !prevHashHex.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("prevHashHex must be 64 lowercase hex chars");
        }
        if (canonical == null) {
            throw new IllegalArgumentException("canonical must not be null");
        }
        final byte[] prev = prevHashHex.getBytes(StandardCharsets.UTF_8);
        final byte[] joined = new byte[prev.length + canonical.length];
        System.arraycopy(prev, 0, joined, 0, prev.length);
        System.arraycopy(canonical, 0, joined, prev.length, canonical.length);
        return hashService.sm3Hex(joined);
    }

    @Override
    public String signEntryHash(final String hashHex) {
        requireHash(hashHex);
        return signService.sign(hashHex.getBytes(StandardCharsets.UTF_8),
                keyService.getAuditSignPrivateKey());
    }

    @Override
    public String auditKeyId() {
        return keyService.getAuditKeyId();
    }

    @Override
    public boolean verifyEntry(final String hashHex, final String signature, final String keyId) {
        requireHash(hashHex);
        if (signature == null || keyId == null) {
            throw new IllegalArgumentException("signature/keyId must not be null");
        }
        final byte[] publicKey = HexFormat.of()
                .parseHex(keyService.getAuditVerifyPublicKeyHex(keyId));
        return signService.verify(hashHex.getBytes(StandardCharsets.UTF_8), signature, publicKey);
    }

    private static void requireHash(final String hashHex) {
        if (hashHex == null) {
            throw new IllegalArgumentException("hashHex must not be null");
        }
    }
}
