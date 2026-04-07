package com.puchain.fep.web.entquery.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 企业信息查询任务 Entity，映射 t_ent_query_task 表。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_ent_query_task")
public class EntQueryTask {

    /** 查询任务 ID（UUID 32位）。 */
    @Id
    @Column(name = "task_id", length = 32)
    private String taskId;

    /** 发起查询的企业 ID。 */
    @Column(name = "enterprise_id", nullable = false, length = 32)
    private String enterpriseId;

    /** 查询类型: REALTIME / BATCH。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "query_type", nullable = false, length = 20)
    private QueryType queryType;

    /** 被查询企业 USCI（18 位）。 */
    @Column(name = "usci", nullable = false, length = 18)
    private String usci;

    /** 被查询企业名称。 */
    @Column(name = "query_target_name", length = 200)
    private String queryTargetName;

    /** 任务状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false, length = 20)
    private QueryTaskStatus taskStatus;

    /** 报文追踪 ID。 */
    @Column(name = "message_id", length = 64)
    private String messageId;

    /** 批量查询文件路径。 */
    @Column(name = "batch_file_path", length = 500)
    private String batchFilePath;

    /** 查询结果摘要 JSON。 */
    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    /** 失败原因。 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 创建时间。 */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 完成时间。 */
    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public EntQueryTask() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取查询任务 ID。
     *
     * @return 任务 ID（UUID 32位）
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * 获取发起查询的企业 ID。
     *
     * @return 企业 ID
     */
    public String getEnterpriseId() {
        return enterpriseId;
    }

    /**
     * 获取查询类型。
     *
     * @return 查询类型枚举
     */
    public QueryType getQueryType() {
        return queryType;
    }

    /**
     * 获取被查询企业 USCI。
     *
     * @return 18位 USCI
     */
    public String getUsci() {
        return usci;
    }

    /**
     * 获取被查询企业名称。
     *
     * @return 企业名称（可为 null）
     */
    public String getQueryTargetName() {
        return queryTargetName;
    }

    /**
     * 获取任务状态。
     *
     * @return 任务状态枚举
     */
    public QueryTaskStatus getTaskStatus() {
        return taskStatus;
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
     * 获取批量查询文件路径。
     *
     * @return 文件路径（可为 null）
     */
    public String getBatchFilePath() {
        return batchFilePath;
    }

    /**
     * 获取查询结果摘要。
     *
     * @return 结果摘要 JSON（可为 null）
     */
    public String getResultSummary() {
        return resultSummary;
    }

    /**
     * 获取失败原因。
     *
     * @return 错误消息（可为 null）
     */
    public String getErrorMessage() {
        return errorMessage;
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

    /**
     * 获取完成时间。
     *
     * @return 完成时间（可为 null）
     */
    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    // ===== Setters =====

    /**
     * 设置查询任务 ID。
     *
     * @param taskId 任务 ID
     */
    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    /**
     * 设置发起查询的企业 ID。
     *
     * @param enterpriseId 企业 ID
     */
    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    /**
     * 设置查询类型。
     *
     * @param queryType 查询类型枚举
     */
    public void setQueryType(final QueryType queryType) {
        this.queryType = queryType;
    }

    /**
     * 设置被查询企业 USCI。
     *
     * @param usci 18位 USCI
     */
    public void setUsci(final String usci) {
        this.usci = usci;
    }

    /**
     * 设置被查询企业名称。
     *
     * @param queryTargetName 企业名称
     */
    public void setQueryTargetName(final String queryTargetName) {
        this.queryTargetName = queryTargetName;
    }

    /**
     * 设置任务状态。
     *
     * @param taskStatus 任务状态枚举
     */
    public void setTaskStatus(final QueryTaskStatus taskStatus) {
        this.taskStatus = taskStatus;
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
     * 设置批量查询文件路径。
     *
     * @param batchFilePath 文件路径
     */
    public void setBatchFilePath(final String batchFilePath) {
        this.batchFilePath = batchFilePath;
    }

    /**
     * 设置查询结果摘要。
     *
     * @param resultSummary 结果摘要 JSON
     */
    public void setResultSummary(final String resultSummary) {
        this.resultSummary = resultSummary;
    }

    /**
     * 设置失败原因。
     *
     * @param errorMessage 错误消息
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
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

    /**
     * 设置完成时间。
     *
     * @param completeTime 完成时间
     */
    public void setCompleteTime(final LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }
}
