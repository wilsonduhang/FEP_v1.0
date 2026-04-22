package com.puchain.fep.processor.routing;

/**
 * PRD §4.7 报文处理模式。
 *
 * <p>See ADR-R-1 (Plan R 2026-04-22): reserved modes dropped per YAGNI; P3 may redesign if needed.
 *
 * <p><b>⚠️ 持久化警告</b>：若未来有系统需持久化本枚举（如 DB 列），<b>必须</b>
 * 存 {@link #name()} 而非 {@link #ordinal()}。插入新值到中间位置会改变
 * ordinal，导致历史数据错位。当前 P2c 范围仅静态内存 Map，无持久化风险。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum ProcessingMode {
    /** 处理模式 1。 */
    MODE_1,
    /** 处理模式 2。 */
    MODE_2,
    /** 处理模式 3。 */
    MODE_3,
    /** 处理模式 5。 */
    MODE_5
}
