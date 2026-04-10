package com.puchain.fep.transport.model;

/**
 * TLQ 节点状态枚举及状态转换规则。
 *
 * <p>状态机规则（参见 PRD v1.3 §3.7）：</p>
 * <ul>
 *   <li>UNKNOWN → ONLINE</li>
 *   <li>ONLINE → OFFLINE</li>
 *   <li>OFFLINE → ONLINE | UNKNOWN</li>
 *   <li>ERROR → UNKNOWN</li>
 *   <li>任意状态 → ERROR</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum NodeState {

    /** 初始/未知状态。 */
    UNKNOWN,

    /** 在线就绪。 */
    ONLINE,

    /** 离线。 */
    OFFLINE,

    /** 错误状态。 */
    ERROR;

    /**
     * 判断当前状态是否可以转换到目标状态。
     *
     * @param target 目标状态，不能为 {@code null}
     * @return 允许转换返回 {@code true}
     */
    public boolean canTransitionTo(final NodeState target) {
        if (target == ERROR) {
            return true;
        }
        return switch (this) {
            case UNKNOWN -> target == ONLINE;
            case ONLINE -> target == OFFLINE;
            case OFFLINE -> target == ONLINE || target == UNKNOWN;
            case ERROR -> target == UNKNOWN;
        };
    }
}
