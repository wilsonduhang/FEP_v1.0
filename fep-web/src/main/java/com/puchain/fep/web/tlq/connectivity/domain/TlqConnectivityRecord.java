package com.puchain.fep.web.tlq.connectivity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * TLQ 连通性测试记录 Entity，映射 t_tlq_connectivity_record 表。
 *
 * <p>参见 PRD v1.3 §5.7.5 连通性测试（FR-WEB-TLQ-CONN）。</p>
 *
 * <p>记录为不可变历史数据，仅支持插入，不支持更新，因此不设 {@code @LastModifiedDate}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_tlq_connectivity_record")
@EntityListeners(AuditingEntityListener.class)
public class TlqConnectivityRecord {

    /** 记录唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "record_id", length = 32)
    private String recordId;

    /** 所属节点 ID（关联 t_tlq_node）。 */
    @Column(name = "node_id", nullable = false, length = 32)
    private String nodeId;

    /** 测试时间。 */
    @Column(name = "test_time", nullable = false)
    private LocalDateTime testTime;

    /** 测试结果。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "test_result", nullable = false, length = 20)
    private ConnectivityTestResult testResult;

    /** 往返时延（毫秒，可为 null）。 */
    @Column(name = "rtt_ms")
    private Integer rttMs;

    /** 错误信息（失败时记录，可为 null）。 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 触发来源（如 MANUAL、SCHEDULED）。 */
    @Column(name = "triggered_by", nullable = false, length = 50)
    private String triggeredBy;

    /** 记录创建时间（自动填充，不可更新）。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 无参构造方法（JPA 要求）。 */
    public TlqConnectivityRecord() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取记录唯一标识。
     *
     * @return 记录 ID（UUID 32位）
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * 获取所属节点 ID。
     *
     * @return 节点 ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 获取测试时间。
     *
     * @return 测试时间
     */
    public LocalDateTime getTestTime() {
        return testTime;
    }

    /**
     * 获取测试结果。
     *
     * @return 测试结果枚举
     */
    public ConnectivityTestResult getTestResult() {
        return testResult;
    }

    /**
     * 获取往返时延（毫秒）。
     *
     * @return RTT（可为 null）
     */
    public Integer getRttMs() {
        return rttMs;
    }

    /**
     * 获取错误信息。
     *
     * @return 错误信息（可为 null）
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 获取触发来源。
     *
     * @return 触发来源字符串
     */
    public String getTriggeredBy() {
        return triggeredBy;
    }

    /**
     * 获取记录创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    // ===== Setters =====

    /**
     * 设置记录唯一标识。
     *
     * @param recordId 记录 ID
     */
    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    /**
     * 设置所属节点 ID。
     *
     * @param nodeId 节点 ID
     */
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 设置测试时间。
     *
     * @param testTime 测试时间
     */
    public void setTestTime(final LocalDateTime testTime) {
        this.testTime = testTime;
    }

    /**
     * 设置测试结果。
     *
     * @param testResult 测试结果枚举
     */
    public void setTestResult(final ConnectivityTestResult testResult) {
        this.testResult = testResult;
    }

    /**
     * 设置往返时延。
     *
     * @param rttMs RTT 毫秒数（可为 null）
     */
    public void setRttMs(final Integer rttMs) {
        this.rttMs = rttMs;
    }

    /**
     * 设置错误信息。
     *
     * @param errorMessage 错误信息（可为 null）
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 设置触发来源。
     *
     * @param triggeredBy 触发来源字符串
     */
    public void setTriggeredBy(final String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}
