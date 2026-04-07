package com.puchain.fep.web.sysmgmt.config.enterprise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 企业主体 Entity，映射 t_sys_enterprise 表。
 *
 * <p>参见 PRD v1.3 §5.10.7.3 企业主体管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_enterprise")
@EntityListeners(AuditingEntityListener.class)
public class SysEnterprise {

    @Id
    @Column(name = "enterprise_id", length = 32)
    private String enterpriseId;

    @Column(name = "enterprise_name", nullable = false, length = 200)
    private String enterpriseName;

    @Column(name = "usci", nullable = false, length = 18, unique = true)
    private String usci;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "key_params", length = 500)
    private String keyParams;

    @Column(name = "sign_file_path", length = 500)
    private String signFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status", nullable = false, length = 20)
    private AuditStatus auditStatus;

    @Column(name = "biz_count", nullable = false)
    private int bizCount;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysEnterprise() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取企业主体唯一标识。
     *
     * @return 企业主体 ID（UUID 32位）
     */
    public String getEnterpriseId() {
        return enterpriseId;
    }

    /**
     * 获取企业名称。
     *
     * @return 企业名称
     */
    public String getEnterpriseName() {
        return enterpriseName;
    }

    /**
     * 获取统一社会信用代码（USCI）。
     *
     * @return 18位 USCI
     */
    public String getUsci() {
        return usci;
    }

    /**
     * 获取报文内容类型。
     *
     * @return 内容类型（可为 null）
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 获取客户端标识。
     *
     * @return 客户端标识（可为 null）
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 获取密钥参数元数据（非实际密钥内容）。
     *
     * @return 密钥参数描述（可为 null）
     */
    public String getKeyParams() {
        return keyParams;
    }

    /**
     * 获取签名文件路径元数据（非实际文件内容）。
     *
     * @return 签名文件路径（可为 null）
     */
    public String getSignFilePath() {
        return signFilePath;
    }

    /**
     * 获取审核状态。
     *
     * @return 审核状态枚举
     */
    public AuditStatus getAuditStatus() {
        return auditStatus;
    }

    /**
     * 获取关联业务数量。
     *
     * @return 关联业务数量
     */
    public int getBizCount() {
        return bizCount;
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
     * 设置企业主体唯一标识。
     *
     * @param enterpriseId 企业主体 ID
     */
    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    /**
     * 设置企业名称。
     *
     * @param enterpriseName 企业名称
     */
    public void setEnterpriseName(final String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    /**
     * 设置统一社会信用代码。
     *
     * @param usci 18位 USCI
     */
    public void setUsci(final String usci) {
        this.usci = usci;
    }

    /**
     * 设置报文内容类型。
     *
     * @param contentType 内容类型
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * 设置客户端标识。
     *
     * @param clientId 客户端标识
     */
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /**
     * 设置密钥参数元数据。
     *
     * @param keyParams 密钥参数描述
     */
    public void setKeyParams(final String keyParams) {
        this.keyParams = keyParams;
    }

    /**
     * 设置签名文件路径元数据。
     *
     * @param signFilePath 签名文件路径
     */
    public void setSignFilePath(final String signFilePath) {
        this.signFilePath = signFilePath;
    }

    /**
     * 设置审核状态。
     *
     * @param auditStatus 审核状态枚举
     */
    public void setAuditStatus(final AuditStatus auditStatus) {
        this.auditStatus = auditStatus;
    }

    /**
     * 设置关联业务数量。
     *
     * @param bizCount 关联业务数量
     */
    public void setBizCount(final int bizCount) {
        this.bizCount = bizCount;
    }
}
