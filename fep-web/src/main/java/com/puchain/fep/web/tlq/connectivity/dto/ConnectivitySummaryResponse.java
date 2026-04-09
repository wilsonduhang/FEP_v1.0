package com.puchain.fep.web.tlq.connectivity.dto;

import com.puchain.fep.web.tlq.connectivity.domain.ConnectivityTestResult;

import java.time.LocalDateTime;

/**
 * TLQ 连通性测试汇总统计响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.7.5 连通性测试（FR-WEB-TLQ-CONN）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ConnectivitySummaryResponse {

    /** 所属节点 ID。 */
    private String nodeId;

    /** 最近一次测试结果（可为 null，无历史记录时）。 */
    private ConnectivityTestResult lastResult;

    /** 最近一次测试时间（可为 null，无历史记录时）。 */
    private LocalDateTime lastTestTime;

    /** 测试总次数。 */
    private long totalTests;

    /** 成功次数。 */
    private long successCount;

    /** 成功率（百分比，0-100.0）。 */
    private double successRate;

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
     * 获取最近一次测试结果。
     *
     * @return 测试结果枚举（可为 null）
     */
    public ConnectivityTestResult getLastResult() {
        return lastResult;
    }

    /**
     * 设置最近一次测试结果。
     *
     * @param lastResult 测试结果枚举（可为 null）
     */
    public void setLastResult(final ConnectivityTestResult lastResult) {
        this.lastResult = lastResult;
    }

    /**
     * 获取最近一次测试时间。
     *
     * @return 测试时间（可为 null）
     */
    public LocalDateTime getLastTestTime() {
        return lastTestTime;
    }

    /**
     * 设置最近一次测试时间。
     *
     * @param lastTestTime 测试时间（可为 null）
     */
    public void setLastTestTime(final LocalDateTime lastTestTime) {
        this.lastTestTime = lastTestTime;
    }

    /**
     * 获取测试总次数。
     *
     * @return 总次数
     */
    public long getTotalTests() {
        return totalTests;
    }

    /**
     * 设置测试总次数。
     *
     * @param totalTests 总次数
     */
    public void setTotalTests(final long totalTests) {
        this.totalTests = totalTests;
    }

    /**
     * 获取成功次数。
     *
     * @return 成功次数
     */
    public long getSuccessCount() {
        return successCount;
    }

    /**
     * 设置成功次数。
     *
     * @param successCount 成功次数
     */
    public void setSuccessCount(final long successCount) {
        this.successCount = successCount;
    }

    /**
     * 获取成功率（百分比）。
     *
     * @return 成功率（0-100.0）
     */
    public double getSuccessRate() {
        return successRate;
    }

    /**
     * 设置成功率（百分比）。
     *
     * @param successRate 成功率（0-100.0）
     */
    public void setSuccessRate(final double successRate) {
        this.successRate = successRate;
    }
}
