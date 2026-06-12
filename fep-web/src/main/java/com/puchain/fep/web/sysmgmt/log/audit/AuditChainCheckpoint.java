package com.puchain.fep.web.sysmgmt.log.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 审计链校验 checkpoint 锚（单行，PK 固定 {@link #SINGLETON_ID}；EFF-S5-1）。
 *
 * <p>持久化 + SM2 签名锚：签名输入为域分隔串 {@code audit-checkpoint:<seq>:<hash>}
 * （与行签名输入空间不相交，防复制行签名伪造，Plan 抉择④）。删尾攻击经
 * "链尾 &lt; verified_until_seq → TRUNCATION" 可检（S5 抉择⑩ 升级）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "audit_chain_checkpoint")
public class AuditChainCheckpoint {

    /** 单行锚固定主键。 */
    public static final String SINGLETON_ID = "SINGLETON";

    @Id
    @Column(name = "id", length = 16)
    private String id = SINGLETON_ID;

    /** 已验证至链 seq（含）。 */
    @Column(name = "verified_until_seq", nullable = false)
    private Long verifiedUntilSeq;

    /** 锚行 hash（t_sys_operation_log.seq=verified_until_seq 行）。 */
    @Column(name = "anchor_hash", nullable = false, length = 64)
    private String anchorHash;

    /** checkpoint SM2 裸签 Base64（域分隔输入；mock 域占位串）。 */
    @Column(name = "checkpoint_signature", nullable = false, length = 120)
    private String checkpointSignature;

    /** 签名时审计密钥版本。 */
    @Column(name = "sign_key_id", nullable = false, length = 64)
    private String signKeyId;

    /** 推进时刻。 */
    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    // ===== 访问器（字段 Javadoc 表意；checkstyle allowMissingPropertyJavadoc=true
    // 豁免 property 方法，按 S5 SysOperationLog 完整性字段段同形态） =====

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Long getVerifiedUntilSeq() {
        return verifiedUntilSeq;
    }

    public void setVerifiedUntilSeq(final Long verifiedUntilSeq) {
        this.verifiedUntilSeq = verifiedUntilSeq;
    }

    public String getAnchorHash() {
        return anchorHash;
    }

    public void setAnchorHash(final String anchorHash) {
        this.anchorHash = anchorHash;
    }

    public String getCheckpointSignature() {
        return checkpointSignature;
    }

    public void setCheckpointSignature(final String checkpointSignature) {
        this.checkpointSignature = checkpointSignature;
    }

    public String getSignKeyId() {
        return signKeyId;
    }

    public void setSignKeyId(final String signKeyId) {
        this.signKeyId = signKeyId;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(final LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
