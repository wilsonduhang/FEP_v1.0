package com.puchain.fep.web.tlq.connectivity.dto;

import com.puchain.fep.web.tlq.connectivity.domain.ConnectivityTestResult;
import com.puchain.fep.web.tlq.connectivity.domain.TlqConnectivityRecord;

import java.time.LocalDateTime;

/**
 * TLQ 连通性测试历史记录响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.7.5 连通性测试（FR-WEB-TLQ-CONN）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ConnectivityRecordResponse {

    /** 记录唯一标识。 */
    private String recordId;

    /** 所属节点 ID。 */
    private String nodeId;

    /** 测试时间。 */
    private LocalDateTime testTime;

    /** 测试结果。 */
    private ConnectivityTestResult testResult;

    /** 往返时延（毫秒，可为 null）。 */
    private Integer rttMs;

    /** 错误信息（可为 null）。 */
    private String errorMessage;

    /** 触发来源。 */
    private String triggeredBy;

    /**
     * 从 Entity 构建响应 DTO。
     *
     * @param entity 连通性记录 Entity
     * @return 响应 DTO
     */
    public static ConnectivityRecordResponse fromEntity(final TlqConnectivityRecord entity) {
        ConnectivityRecordResponse dto = new ConnectivityRecordResponse();
        dto.setRecordId(entity.getRecordId());
        dto.setNodeId(entity.getNodeId());
        dto.setTestTime(entity.getTestTime());
        dto.setTestResult(entity.getTestResult());
        dto.setRttMs(entity.getRttMs());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setTriggeredBy(entity.getTriggeredBy());
        return dto;
    }

    /**
     * 获取记录唯一标识。
     *
     * @return 记录 ID
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * 设置记录唯一标识。
     *
     * @param recordId 记录 ID
     */
    public void setRecordId(final String recordId) {
        this.recordId = recordId;
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
     * 设置所属节点 ID。
     *
     * @param nodeId 节点 ID
     */
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
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
     * 设置测试时间。
     *
     * @param testTime 测试时间
     */
    public void setTestTime(final LocalDateTime testTime) {
        this.testTime = testTime;
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
     * 设置测试结果。
     *
     * @param testResult 测试结果枚举
     */
    public void setTestResult(final ConnectivityTestResult testResult) {
        this.testResult = testResult;
    }

    /**
     * 获取往返时延。
     *
     * @return RTT 毫秒数（可为 null）
     */
    public Integer getRttMs() {
        return rttMs;
    }

    /**
     * 设置往返时延。
     *
     * @param rttMs RTT 毫秒数
     */
    public void setRttMs(final Integer rttMs) {
        this.rttMs = rttMs;
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
     * 设置错误信息。
     *
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
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
     * 设置触发来源。
     *
     * @param triggeredBy 触发来源字符串
     */
    public void setTriggeredBy(final String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }
}
