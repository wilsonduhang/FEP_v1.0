package com.puchain.fep.web.sysmgmt.config.enterprise.dto;

import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterprise;

import java.time.LocalDateTime;

/**
 * 企业主体响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.3 企业主体管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class EnterpriseResponse {

    private String enterpriseId;
    private String enterpriseName;
    private String usci;
    private String contentType;
    private String clientId;
    private String keyParams;
    private String signFilePath;
    private String auditStatus;
    private int bizCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 企业主体 Entity
     * @return 响应 DTO
     */
    public static EnterpriseResponse from(final SysEnterprise entity) {
        EnterpriseResponse resp = new EnterpriseResponse();
        resp.enterpriseId = entity.getEnterpriseId();
        resp.enterpriseName = entity.getEnterpriseName();
        resp.usci = entity.getUsci();
        resp.contentType = entity.getContentType();
        resp.clientId = entity.getClientId();
        resp.keyParams = entity.getKeyParams();
        resp.signFilePath = entity.getSignFilePath();
        resp.auditStatus = entity.getAuditStatus() != null
                ? entity.getAuditStatus().name() : null;
        resp.bizCount = entity.getBizCount();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取企业主体 ID。
     *
     * @return 企业主体 ID
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
     * 获取统一社会信用代码。
     *
     * @return USCI
     */
    public String getUsci() {
        return usci;
    }

    /**
     * 获取报文内容类型。
     *
     * @return 内容类型
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 获取客户端标识。
     *
     * @return 客户端标识
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 获取密钥参数元数据。
     *
     * @return 密钥参数描述
     */
    public String getKeyParams() {
        return keyParams;
    }

    /**
     * 获取签名文件路径元数据。
     *
     * @return 签名文件路径
     */
    public String getSignFilePath() {
        return signFilePath;
    }

    /**
     * 获取审核状态字符串。
     *
     * @return 审核状态（PENDING/APPROVED/REJECTED）
     */
    public String getAuditStatus() {
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
}
