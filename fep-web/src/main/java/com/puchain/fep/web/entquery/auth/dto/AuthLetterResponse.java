package com.puchain.fep.web.entquery.auth.dto;

import com.puchain.fep.web.entquery.auth.domain.EntAuthLetter;

/**
 * 授权书响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class AuthLetterResponse {

    private String letterId;
    private String enterpriseId;
    private String authType;
    private String authScope;
    private String authorizedUsci;
    private String authorizedName;
    private String filePath;
    private String letterStatus;
    private String messageId;
    private String submitTime;
    private String ackTime;
    private String rejectReason;
    private String createTime;
    private String updateTime;

    /**
     * 从 Entity 构建响应 DTO。
     *
     * @param entity 授权书实体
     * @return 响应 DTO
     */
    public static AuthLetterResponse from(final EntAuthLetter entity) {
        AuthLetterResponse resp = new AuthLetterResponse();
        resp.letterId = entity.getLetterId();
        resp.enterpriseId = entity.getEnterpriseId();
        resp.authType = entity.getAuthType().name();
        resp.authScope = entity.getAuthScope();
        resp.authorizedUsci = entity.getAuthorizedUsci();
        resp.authorizedName = entity.getAuthorizedName();
        resp.filePath = entity.getFilePath();
        resp.letterStatus = entity.getLetterStatus().name();
        resp.messageId = entity.getMessageId();
        resp.submitTime = entity.getSubmitTime() != null
                ? entity.getSubmitTime().toString() : null;
        resp.ackTime = entity.getAckTime() != null
                ? entity.getAckTime().toString() : null;
        resp.rejectReason = entity.getRejectReason();
        resp.createTime = entity.getCreateTime() != null
                ? entity.getCreateTime().toString() : null;
        resp.updateTime = entity.getUpdateTime() != null
                ? entity.getUpdateTime().toString() : null;
        return resp;
    }

    // ===== Getters =====

    /**
     * 获取授权书 ID。
     *
     * @return 授权书 ID
     */
    public String getLetterId() {
        return letterId;
    }

    /**
     * 获取企业 ID。
     *
     * @return 企业 ID
     */
    public String getEnterpriseId() {
        return enterpriseId;
    }

    /**
     * 获取授权书类型。
     *
     * @return 授权书类型名称
     */
    public String getAuthType() {
        return authType;
    }

    /**
     * 获取授权范围。
     *
     * @return 授权范围描述
     */
    public String getAuthScope() {
        return authScope;
    }

    /**
     * 获取被授权企业 USCI。
     *
     * @return USCI
     */
    public String getAuthorizedUsci() {
        return authorizedUsci;
    }

    /**
     * 获取被授权企业名称。
     *
     * @return 企业名称
     */
    public String getAuthorizedName() {
        return authorizedName;
    }

    /**
     * 获取授权书文件路径。
     *
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 获取授权书状态。
     *
     * @return 授权书状态名称
     */
    public String getLetterStatus() {
        return letterStatus;
    }

    /**
     * 获取报文追踪 ID。
     *
     * @return 报文 ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 获取提交时间。
     *
     * @return 提交时间字符串
     */
    public String getSubmitTime() {
        return submitTime;
    }

    /**
     * 获取平台确认时间。
     *
     * @return 确认时间字符串
     */
    public String getAckTime() {
        return ackTime;
    }

    /**
     * 获取拒绝原因。
     *
     * @return 拒绝原因
     */
    public String getRejectReason() {
        return rejectReason;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间字符串
     */
    public String getCreateTime() {
        return createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间字符串
     */
    public String getUpdateTime() {
        return updateTime;
    }
}
