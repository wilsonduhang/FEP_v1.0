package com.puchain.fep.web.entquery.result.dto;

import com.puchain.fep.web.entquery.result.domain.EntQueryResult;

/**
 * 查询结果响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class QueryResultResponse {

    private String resultId;
    private String taskId;
    private String resultUsci;
    private String enterpriseName;
    private String resultData;
    private String resultStatus;
    private String errorCode;
    private String errorMessage;
    private String createTime;

    /**
     * 从 Entity 构建响应 DTO。
     *
     * @param entity 查询结果实体
     * @return 响应 DTO
     */
    public static QueryResultResponse from(final EntQueryResult entity) {
        QueryResultResponse resp = new QueryResultResponse();
        resp.resultId = entity.getResultId();
        resp.taskId = entity.getTaskId();
        resp.resultUsci = entity.getResultUsci();
        resp.enterpriseName = entity.getEnterpriseName();
        resp.resultData = entity.getResultData();
        resp.resultStatus = entity.getResultStatus() != null
                ? entity.getResultStatus().name() : null;
        resp.errorCode = entity.getErrorCode();
        resp.errorMessage = entity.getErrorMessage();
        resp.createTime = entity.getCreateTime() != null
                ? entity.getCreateTime().toString() : null;
        return resp;
    }

    // ===== Getters =====

    /**
     * 获取结果 ID。
     *
     * @return 结果 ID
     */
    public String getResultId() {
        return resultId;
    }

    /**
     * 获取任务 ID。
     *
     * @return 任务 ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * 获取结果对应 USCI。
     *
     * @return USCI
     */
    public String getResultUsci() {
        return resultUsci;
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
     * 获取完整结果数据。
     *
     * @return 结果 JSON
     */
    public String getResultData() {
        return resultData;
    }

    /**
     * 获取结果状态。
     *
     * @return NORMAL 或 ERROR
     */
    public String getResultStatus() {
        return resultStatus;
    }

    /**
     * 获取错误码。
     *
     * @return HNDEMP 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误描述。
     *
     * @return 错误消息
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间字符串
     */
    public String getCreateTime() {
        return createTime;
    }
}
