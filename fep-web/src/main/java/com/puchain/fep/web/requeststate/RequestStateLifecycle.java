package com.puchain.fep.web.requeststate;

/**
 * 请求生命周期状态机（5 状态）。S2 request-state tracking 子系统。
 *
 * <p>追踪一条 outbound 请求从发送到 HNDEMP 结果返回的完整生命周期。correlation key =
 * 8 位业务 transitionNo（outbound enqueue 值与 inbound 归一值同源）。状态流转：</p>
 *
 * <pre>
 *   CREATED ──&gt; SENT ──&gt; RESULT_RECEIVED   (happy path 终态)
 *                 │
 *                 └──&gt; STUCK                (reaper: SENT 超 TTL 仍无结果)
 *   CREATED/SENT ─&gt; FAILED                  (outbound 永久失败 / DLQ)
 * </pre>
 *
 * <p>{@link #RESULT_RECEIVED} 是唯一的 happy 终态；{@link #FAILED} 与 {@link #STUCK}
 * 为旁支终态。{@code correlation_blocked=true}（结构性永等不到匹配，如 P3 Phase2
 * platPayNo 占位 3115 链，见 {@link BlockedMessageTypes}）的请求被 reaper 排除，不会被
 * 标记为 {@link #STUCK}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum RequestStateLifecycle {

    /** 请求已入队（outbound enqueue），尚未发往 HNDEMP。初始状态。 */
    CREATED,

    /** 请求已成功通过 TLQ 发往 HNDEMP，等待结果返回。 */
    SENT,

    /** 已收到 HNDEMP 结果并按 transitionNo 匹配回填。happy path 终态。 */
    RESULT_RECEIVED,

    /** outbound 永久失败或进入 DLQ。旁支终态。 */
    FAILED,

    /** reaper 检测到 {@link #SENT} 超过 TTL 仍无结果（排除 correlation_blocked 行）。旁支终态。 */
    STUCK
}
