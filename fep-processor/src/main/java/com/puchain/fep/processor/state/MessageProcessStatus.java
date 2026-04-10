package com.puchain.fep.processor.state;

/**
 * 报文处理生命周期状态。PRD §4.7 模式1 同步处理。
 *
 * <p>合法状态转移由 {@code MessageStateMachine}（Task 5）守护：
 * <pre>
 * RECEIVED → VALIDATED → PROCESSING → COMPLETED
 *     ↓         ↓            ↓
 *    FAILED   FAILED       FAILED
 * </pre>
 * 任一非终态均可转向 FAILED；已进入终态（COMPLETED/FAILED）不可再次转移。</p>
 */
public enum MessageProcessStatus {

    /** 已接收，尚未进入校验。 */
    RECEIVED,

    /** 已通过 XSD + 业务规则校验。 */
    VALIDATED,

    /** 正在执行业务处理。 */
    PROCESSING,

    /** 处理成功（终态）。 */
    COMPLETED,

    /** 处理失败（终态），必须携带 errorCode + errorMessage。 */
    FAILED;

    /**
     * 判断当前状态是否为终态（{@link #COMPLETED} 或 {@link #FAILED}）。
     *
     * <p>终态不允许再次状态转移，状态机必须拒绝任何从终态出发的转换。</p>
     *
     * @return 终态返回 {@code true}，非终态返回 {@code false}
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
