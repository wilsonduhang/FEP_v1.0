package com.puchain.fep.web.tlq.node.domain;

/**
 * TLQ 节点状态枚举。
 *
 * <p>状态机：UNKNOWN → ONLINE ↔ OFFLINE。
 * 参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TlqNodeStatus {

    /** 上线（节点可用）。 */
    ONLINE,

    /** 下线（节点不可用）。 */
    OFFLINE,

    /** 未知（初始状态，心跳尚未建立）。 */
    UNKNOWN;

    /**
     * 校验从当前状态迁移到目标状态是否合法。
     *
     * <p>允许的迁移：
     * <ul>
     *   <li>UNKNOWN → ONLINE</li>
     *   <li>ONLINE  → OFFLINE</li>
     *   <li>OFFLINE → ONLINE</li>
     * </ul>
     * </p>
     *
     * @param target 目标状态
     * @return 是否允许迁移
     */
    public boolean canTransitionTo(final TlqNodeStatus target) {
        return switch (this) {
            case UNKNOWN -> target == ONLINE;
            case ONLINE -> target == OFFLINE;
            case OFFLINE -> target != OFFLINE && target != UNKNOWN;
        };
    }
}
