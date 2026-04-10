package com.puchain.fep.converter.type;

/**
 * 报文流向。参见 PRD v1.3 §4.1-4.5 方向列。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum MessageDirection {
    /** 外联机构 → HNDEMP */
    OUTBOUND,
    /** HNDEMP → 外联机构 */
    INBOUND,
    /** 双向：视双角色切换而定 */
    BIDIRECTIONAL
}
