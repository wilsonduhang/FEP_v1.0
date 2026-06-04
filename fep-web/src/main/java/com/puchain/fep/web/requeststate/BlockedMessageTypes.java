package com.puchain.fep.web.requeststate;

import com.puchain.fep.converter.type.MessageType;

import java.util.Set;

/**
 * 结构性 correlation 阻塞报文集合判定（S2 request-state {@code correlation_blocked} 列来源）。
 *
 * <p>「结构性阻塞」= 经本 outbound 流发出后，因 correlation key（8 位业务 transitionNo）在
 * 当前实现下<strong>永远等不到匹配的 inbound 结果</strong>而注定卡在 {@link RequestStateLifecycle#SENT}
 * 的报文类型。这类请求若不隔离，会污染 reaper 的 STUCK 计数（红线
 * {@code audit_maturity_label_needs_prd_trace}：STUCK consumer surface 须真消费而非已知缺口噪声）。</p>
 *
 * <h3>逐 msgNo trace 依据（按 ADR 当前 Status，禁字面外推）</h3>
 * <ul>
 *   <li><strong>3115</strong>（资金清算信息指令及回执）— <em>纳入</em>。P3 Phase 2 platPayNo →
 *       人行 instruction_id 升级<strong>仍全 BLOCKED</strong>（见
 *       {@code docs/decisions/2026-05-05-inbound-realhead-extraction-blocked.md} §3.2 + §R3 Addendum
 *       「P3 Phase 2 仍全 BLOCKED」）：outbound 以 platPayNo 占位 correlation，inbound 3115 return
 *       需带回人行真 instruction_id 才能匹配，而该协议依赖 ③ 安全工程师 + PRD instruction_id 协议
 *       + 真 broker 3115 return fixture 三项外部条件，均未满足。</li>
 *   <li><strong>R3 类型（3115/3116/3107/3108 inbound transitionNo 等）</strong>— <em>不纳入</em>。
 *       R3 占位缺陷 2026-06-01 已 DONE（{@code InboundTransitionNoExtractor.extract()} 从业务头提
 *       真值 + {@code deriveTransitionNo} 仅兜底），transitionNo correlation 不再结构性阻塞。</li>
 *   <li><strong>9006/9008</strong>（节点登录/登出）— <em>不纳入</em>。transitionNo 由
 *       {@code deriveTransitionNo}（msgId 末 8 位）派生为 8 位，两侧同源可匹配，非阻塞。</li>
 * </ul>
 *
 * <p>当前集合仅含 {@code 3115}。新增结构性阻塞类型时在 {@link #BLOCKED_MSG_NOS} 增项并补 ADR
 * trace 注释。集合空亦为合法状态（当所有 BLOCKED ADR 解锁后）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class BlockedMessageTypes {

    /**
     * 结构性永等不到匹配的报文号集合（4 位 msgNo）。
     *
     * <p>当前仅 {@code 3115}（P3 Phase 2 platPayNo 占位，ADR
     * {@code 2026-05-05-inbound-realhead-extraction-blocked} §3.2 仍 BLOCKED）。</p>
     */
    private static final Set<String> BLOCKED_MSG_NOS = Set.of("3115");

    private BlockedMessageTypes() {
    }

    /**
     * 判定报文类型是否结构性 correlation 阻塞。
     *
     * @param type 报文类型，非空
     * @return {@code true} 表示该类型的 outbound 请求永等不到匹配 inbound 结果（CREATED 时置
     *         {@code correlation_blocked=true}，reaper 排除 STUCK 检测）
     */
    public static boolean isBlocked(final MessageType type) {
        return type != null && BLOCKED_MSG_NOS.contains(type.msgNo());
    }

    /**
     * 判定报文号是否结构性 correlation 阻塞（字符串重载，供仅持有 4 位 msgNo 的调用方）。
     *
     * @param msgNo 4 位报文号，可空
     * @return {@code true} 表示结构性阻塞
     */
    public static boolean isBlocked(final String msgNo) {
        return msgNo != null && BLOCKED_MSG_NOS.contains(msgNo);
    }
}
