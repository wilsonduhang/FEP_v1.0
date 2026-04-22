package com.puchain.fep.processor.routing;

/**
 * PRD §4.7 报文处理模式。模式 4 / 6 当前 §4.6 未出现，保留供未来扩展。
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
    /** 处理模式 4（§4.6 未出现，保留）。 */
    MODE_4,
    /** 处理模式 5。 */
    MODE_5,
    /** 处理模式 6（§4.6 未出现，保留）。 */
    MODE_6
}
