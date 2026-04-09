package com.puchain.fep.web.tlq.connectivity.domain;

/**
 * TLQ 连通性测试结果枚举。
 *
 * <p>对应 PRD v1.3 §5.7.5 连通性测试功能，记录每次测试的最终状态。</p>
 *
 * <ul>
 *   <li>{@link #SUCCESS} — 测试成功，节点可达</li>
 *   <li>{@link #FAILURE} — 测试失败，节点不可达或返回错误</li>
 *   <li>{@link #TIMEOUT} — 测试超时，未在规定时间内收到响应</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum ConnectivityTestResult {

    /** 连通性测试成功。 */
    SUCCESS,

    /** 连通性测试失败。 */
    FAILURE,

    /** 连通性测试超时。 */
    TIMEOUT
}
