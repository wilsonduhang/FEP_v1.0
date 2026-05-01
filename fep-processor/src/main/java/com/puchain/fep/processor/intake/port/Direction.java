package com.puchain.fep.processor.intake.port;

/**
 * 报文流向枚举（intake.port 跨模块契约）。
 *
 * <p>当前 P4 数据采集层场景仅出现 {@link #OUTBOUND}（采集 → 出站队列）。
 * P5+ 队列消费链路（入站接收）若需复用本契约时再扩展。
 *
 * <p>显式枚举字段（而非 Boolean 或字符串字面量）的好处：
 * <ul>
 *   <li>类型安全 — 消费方 switch 全枚举值时编译期可校验完整性</li>
 *   <li>可追加性 — 未来新增方向无需破坏现有 Envelope 字段类型</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum Direction {

    /** 出站方向：采集层 → fep-web 队列 → P5+ 消费链路。 */
    OUTBOUND
}
