package com.puchain.fep.web.entquery.result.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 企业信息查询结果 Entity，映射 t_ent_query_result 表。
 *
 * <p>每条记录对应一个查询任务的一条结果（按 USCI 维度），
 * 由后端收到 HNDEMP 2001/2103 回执后写入。
 * 参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_ent_query_result")
public class EntQueryResult {

    /** 结果 ID（UUID 32位）。 */
    @Id
    @Column(name = "result_id", length = 32)
    private String resultId;

    /** 关联查询任务 ID。 */
    @Column(name = "task_id", nullable = false, length = 32)
    private String taskId;

    /** 结果对应 USCI（18位）。 */
    @Column(name = "result_usci", nullable = false, length = 18)
    private String resultUsci;

    /** 企业名称。 */
    @Column(name = "enterprise_name", length = 200)
    private String enterpriseName;

    /** 完整结果 JSON。 */
    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    /** 结果状态: NORMAL / ERROR。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private ResultStatus resultStatus;

    /** HNDEMP 错误码。 */
    @Column(name = "error_code", length = 20)
    private String errorCode;

    /** 错误描述。 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 创建时间。 */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public EntQueryResult() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取结果 ID。
     *
     * @return 结果 ID（UUID 32位）
     */
    public String getResultId() {
        return resultId;
    }

    /**
     * 获取关联查询任务 ID。
     *
     * @return 任务 ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * 获取结果对应 USCI。
     *
     * @return 18位 USCI
     */
    public String getResultUsci() {
        return resultUsci;
    }

    /**
     * 获取企业名称。
     *
     * @return 企业名称（可为 null）
     */
    public String getEnterpriseName() {
        return enterpriseName;
    }

    /**
     * 获取完整结果 JSON。
     *
     * @return 结果数据（可为 null）
     */
    public String getResultData() {
        return resultData;
    }

    /**
     * 获取结果状态。
     *
     * @return NORMAL 或 ERROR
     */
    public ResultStatus getResultStatus() {
        return resultStatus;
    }

    /**
     * 获取 HNDEMP 错误码。
     *
     * @return 错误码（可为 null）
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误描述。
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

    // ===== Setters =====

    /**
     * 设置结果 ID。
     *
     * @param resultId 结果 ID
     */
    public void setResultId(final String resultId) {
        this.resultId = resultId;
    }

    /**
     * 设置关联查询任务 ID。
     *
     * @param taskId 任务 ID
     */
    public void setTaskId(final String taskId) {
        this.taskId = taskId;
    }

    /**
     * 设置结果对应 USCI。
     *
     * @param resultUsci 18位 USCI
     */
    public void setResultUsci(final String resultUsci) {
        this.resultUsci = resultUsci;
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
     * 设置完整结果 JSON。
     *
     * @param resultData 结果数据
     */
    public void setResultData(final String resultData) {
        this.resultData = resultData;
    }

    /**
     * 设置结果状态。
     *
     * @param resultStatus NORMAL 或 ERROR
     */
    public void setResultStatus(final ResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    /**
     * 设置 HNDEMP 错误码。
     *
     * @param errorCode 错误码
     */
    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 设置错误描述。
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
}
