package com.puchain.fep.security.api;

/**
 * 审计日志完整性原语（SM3 hash 链 + SM2 行签名，架构 §1219 不可篡改）。
 *
 * <p>hash = SM3(prevHashHex ∥ canonical)；signature = SM2（SM3withSM2 裸签）over hashHex。
 * canonical 规范化由调用方（fep-web AuditCanonicalizer）负责；本 SPI 不感知日志字段结构。
 * 签名密钥 = {@code fep.security.sm2.audit-*} 段（KeyService audit accessors，GM S5）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface AuditIntegrityService {

    /** 链首前驱哈希（64 个 '0'）。 */
    String GENESIS_PREV_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * 计算链式行哈希。
     *
     * @param prevHashHex 前一行 hash（64 小写 hex；链首用 {@link #GENESIS_PREV_HASH}），非 null
     * @param canonical   本行规范化字节，非 null
     * @return 64 字符小写 hex
     * @throws IllegalArgumentException 入参 null 或 prevHashHex 非 64 小写 hex
     */
    String computeEntryHash(String prevHashHex, byte[] canonical);

    /**
     * 用活跃审计私钥签名行哈希。
     *
     * @param hashHex 行 hash（64 hex），非 null
     * @return 签名串（impl = Base64(raw r∥s 64 字节)；mock = 占位串）
     * @throws IllegalArgumentException hashHex 为 null
     * @throws IllegalStateException    审计密钥段未配置（impl provider）
     */
    String signEntryHash(String hashHex);

    /**
     * 当前活跃审计密钥版本（落 sign_key_id 列）。
     *
     * @return 审计密钥版本号
     * @throws IllegalStateException 审计密钥段未配置（impl provider）
     */
    String auditKeyId();

    /**
     * 按版本验签行哈希。
     *
     * @param hashHex   行 hash，非 null
     * @param signature 签名串，非 null
     * @param keyId     签名时密钥版本，非 null
     * @return 验签通过 true
     * @throws IllegalArgumentException keyId 未知或入参 null
     * @throws IllegalStateException    审计密钥段未配置（impl provider）——配置错误信号，
     *                                  与 {@link #signEntryHash} 对称；verifier 不捕获此异常
     */
    boolean verifyEntry(String hashHex, String signature, String keyId);
}
