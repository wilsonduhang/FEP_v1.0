package com.puchain.fep.web.entquery.task.dto;

import com.puchain.fep.web.entquery.task.domain.EntQueryTask;

/**
 * 查询任务响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class QueryTaskResponse {

    private String taskId;
    private String enterpriseId;
    private String queryType;
    private String usci;
    private String queryTargetName;
    private String taskStatus;
    private String messageId;
    private String batchFilePath;
    private String resultSummary;
    private String errorMessage;
    private String createTime;
    private String updateTime;
    private String completeTime;

    /**
     * 从 Entity 构建响应 DTO。
     *
     * @param entity 查询任务实体
     * @return 响应 DTO
     */
    public static QueryTaskResponse from(final EntQueryTask entity) {
        QueryTaskResponse resp = new QueryTaskResponse();
        resp.taskId = entity.getTaskId();
        resp.enterpriseId = entity.getEnterpriseId();
        resp.queryType = entity.getQueryType().name();
        resp.usci = entity.getUsci();
        resp.queryTargetName = entity.getQueryTargetName();
        resp.taskStatus = entity.getTaskStatus().name();
        resp.messageId = entity.getMessageId();
        resp.batchFilePath = entity.getBatchFilePath();
        resp.resultSummary = entity.getResultSummary();
        resp.errorMessage = entity.getErrorMessage();
        resp.createTime = entity.getCreateTime() != null
                ? entity.getCreateTime().toString() : null;
        resp.updateTime = entity.getUpdateTime() != null
                ? entity.getUpdateTime().toString() : null;
        resp.completeTime = entity.getCompleteTime() != null
                ? entity.getCompleteTime().toString() : null;
        return resp;
    }

    // ===== Getters =====

    /**
     * 获取任务 ID。
     *
     * @return 任务 ID
     */
    public String getTaskId() {
        return taskId;
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
     * 获取查询类型。
     *
     * @return 查询类型名称
     */
    public String getQueryType() {
        return queryType;
    }

    /**
     * 获取 USCI。
     *
     * @return USCI
     */
    public String getUsci() {
        return usci;
    }

    /**
     * 获取被查询企业名称。
     *
     * @return 企业名称
     */
    public String getQueryTargetName() {
        return queryTargetName;
    }

    /**
     * 获取任务状态。
     *
     * @return 任务状态名称
     */
    public String getTaskStatus() {
        return taskStatus;
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
     * 获取批量文件路径。
     *
     * @return 文件路径
     */
    public String getBatchFilePath() {
        return batchFilePath;
    }

    /**
     * 获取结果摘要。
     *
     * @return 结果摘要 JSON
     */
    public String getResultSummary() {
        return resultSummary;
    }

    /**
     * 获取错误消息。
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

    /**
     * 获取更新时间。
     *
     * @return 更新时间字符串
     */
    public String getUpdateTime() {
        return updateTime;
    }

    /**
     * 获取完成时间。
     *
     * @return 完成时间字符串
     */
    public String getCompleteTime() {
        return completeTime;
    }
}
