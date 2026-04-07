package com.puchain.fep.web.entquery.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 授权书 Entity，映射 t_ent_auth_letter 表。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_ent_auth_letter")
public class EntAuthLetter {

    /** 授权书 ID（UUID 32位）。 */
    @Id
    @Column(name = "letter_id", length = 32)
    private String letterId;

    /** 发起授权的企业 ID。 */
    @Column(name = "enterprise_id", nullable = false, length = 32)
    private String enterpriseId;

    /** 授权书类型: PAPER / ELECTRONIC。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private AuthType authType;

    /** 授权范围描述。 */
    @Column(name = "auth_scope", length = 500)
    private String authScope;

    /** 被授权企业 USCI（18 位）。 */
    @Column(name = "authorized_usci", nullable = false, length = 18)
    private String authorizedUsci;

    /** 被授权企业名称。 */
    @Column(name = "authorized_name", length = 200)
    private String authorizedName;

    /** 授权书文件路径。 */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** 授权书状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "letter_status", nullable = false, length = 20)
    private LetterStatus letterStatus;

    /** 报文追踪 ID。 */
    @Column(name = "message_id", length = 64)
    private String messageId;

    /** 提交时间。 */
    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    /** 平台确认时间。 */
    @Column(name = "ack_time")
    private LocalDateTime ackTime;

    /** 拒绝原因。 */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 创建时间。 */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public EntAuthLetter() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取授权书 ID。
     *
     * @return 授权书 ID（UUID 32位）
     */
    public String getLetterId() {
        return letterId;
    }

    /**
     * 获取发起授权的企业 ID。
     *
     * @return 企业 ID
     */
    public String getEnterpriseId() {
        return enterpriseId;
    }

    /**
     * 获取授权书类型。
     *
     * @return 授权书类型枚举
     */
    public AuthType getAuthType() {
        return authType;
    }

    /**
     * 获取授权范围。
     *
     * @return 授权范围描述（可为 null）
     */
    public String getAuthScope() {
        return authScope;
    }

    /**
     * 获取被授权企业 USCI。
     *
     * @return 18位 USCI
     */
    public String getAuthorizedUsci() {
        return authorizedUsci;
    }

    /**
     * 获取被授权企业名称。
     *
     * @return 企业名称（可为 null）
     */
    public String getAuthorizedName() {
        return authorizedName;
    }

    /**
     * 获取授权书文件路径。
     *
     * @return 文件路径（可为 null）
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 获取授权书状态。
     *
     * @return 授权书状态枚举
     */
    public LetterStatus getLetterStatus() {
        return letterStatus;
    }

    /**
     * 获取报文追踪 ID。
     *
     * @return 报文 ID（可为 null）
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 获取提交时间。
     *
     * @return 提交时间（可为 null）
     */
    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    /**
     * 获取平台确认时间。
     *
     * @return 确认时间（可为 null）
     */
    public LocalDateTime getAckTime() {
        return ackTime;
    }

    /**
     * 获取拒绝原因。
     *
     * @return 拒绝原因（可为 null）
     */
    public String getRejectReason() {
        return rejectReason;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== Setters =====

    /**
     * 设置授权书 ID。
     *
     * @param letterId 授权书 ID
     */
    public void setLetterId(final String letterId) {
        this.letterId = letterId;
    }

    /**
     * 设置发起授权的企业 ID。
     *
     * @param enterpriseId 企业 ID
     */
    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    /**
     * 设置授权书类型。
     *
     * @param authType 授权书类型枚举
     */
    public void setAuthType(final AuthType authType) {
        this.authType = authType;
    }

    /**
     * 设置授权范围。
     *
     * @param authScope 授权范围描述
     */
    public void setAuthScope(final String authScope) {
        this.authScope = authScope;
    }

    /**
     * 设置被授权企业 USCI。
     *
     * @param authorizedUsci 18位 USCI
     */
    public void setAuthorizedUsci(final String authorizedUsci) {
        this.authorizedUsci = authorizedUsci;
    }

    /**
     * 设置被授权企业名称。
     *
     * @param authorizedName 企业名称
     */
    public void setAuthorizedName(final String authorizedName) {
        this.authorizedName = authorizedName;
    }

    /**
     * 设置授权书文件路径。
     *
     * @param filePath 文件路径
     */
    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    /**
     * 设置授权书状态。
     *
     * @param letterStatus 授权书状态枚举
     */
    public void setLetterStatus(final LetterStatus letterStatus) {
        this.letterStatus = letterStatus;
    }

    /**
     * 设置报文追踪 ID。
     *
     * @param messageId 报文 ID
     */
    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    /**
     * 设置提交时间。
     *
     * @param submitTime 提交时间
     */
    public void setSubmitTime(final LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    /**
     * 设置平台确认时间。
     *
     * @param ackTime 确认时间
     */
    public void setAckTime(final LocalDateTime ackTime) {
        this.ackTime = ackTime;
    }

    /**
     * 设置拒绝原因。
     *
     * @param rejectReason 拒绝原因
     */
    public void setRejectReason(final String rejectReason) {
        this.rejectReason = rejectReason;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
