package com.puchain.fep.processor.routing;

/**
 * 角色视角下报文方向 / 动作语义。
 *
 * <p>FR-MSG-DIR-MAP 的输出维度，共 3 种方向。</p>
 *
 * <p>See ADR-R-1 (Plan R 2026-04-22): reserved ACK values dropped per YAGNI; P3 may redesign if needed.
 *
 * <p><b>⚠️ 持久化警告</b>：若未来有系统需持久化本枚举（如 DB 列），<b>必须</b>
 * 存 {@link #name()} 而非 {@link #ordinal()}。插入新值到中间位置会改变
 * ordinal，导致历史数据错位。当前 P2c 范围仅静态内存 Map，无持久化风险。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum RoleDirection {
    /** 本角色主动发起（含"主动发起(如有平台)"子集）。 */
    OUTBOUND_ACTIVE,
    /** 本角色被动接收（含"被动接收(直送总行)"子集）。 */
    INBOUND_PASSIVE,
    /** 本角色与该报文无关（"不涉及"）。 */
    NOT_APPLICABLE
}
