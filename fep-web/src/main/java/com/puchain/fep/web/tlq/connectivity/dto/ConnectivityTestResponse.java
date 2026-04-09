package com.puchain.fep.web.tlq.connectivity.dto;

import com.puchain.fep.web.tlq.connectivity.domain.ConnectivityTestResult;

import java.time.LocalDateTime;

/**
 * TLQ 连通性测试触发响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.7.5 连通性测试（FR-WEB-TLQ-CONN）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ConnectivityTestResponse {

    /** 记录唯一标识。 */
    private String recordId;

    /** 所属节点 ID。 */
    private String nodeId;

    /** 测试结果。 */
    private ConnectivityTestResult result;

    /** 往返时延（毫秒，可为 null）。 */
    private Integer rttMs;

    /** 消息描述。 */
    private String message;

    /** 测试时间。 */
    private LocalDateTime testTime;

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
     * 获取测试结果。
     *
     * @return 测试结果枚举
     */
    public ConnectivityTestResult getResult() {
        return result;
    }

    /**
     * 设置测试结果。
     *
     * @param result 测试结果枚举
     */
    public void setResult(final ConnectivityTestResult result) {
        this.result = result;
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
     * 获取消息描述。
     *
     * @return 消息字符串
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置消息描述。
     *
     * @param message 消息字符串
     */
    public void setMessage(final String message) {
        this.message = message;
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
}
