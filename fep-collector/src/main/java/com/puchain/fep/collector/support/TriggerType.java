package com.puchain.fep.collector.support;

/**
 * 采集运行触发方式枚举。
 *
 * <p>语义：
 * <ul>
 *   <li>{@link #SCHEDULED} — 由 Cron 调度器自动触发（{@code fep.collector.adapters[*].cron}）</li>
 *   <li>{@link #MANUAL}    — 由管理 Web / 运维接口手动触发（用于补采 / 排障）</li>
 * </ul>
 *
 * <p>触发方式记录在 {@code CollectionRunContext} 中，便于追溯与审计区分。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TriggerType {

    /** 由 Cron 调度器自动触发。 */
    SCHEDULED,

    /** 由管理 Web / 运维接口手动触发。 */
    MANUAL
}
