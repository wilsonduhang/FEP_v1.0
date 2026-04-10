package com.puchain.fep.converter.type;

/**
 * 报文类别。参见 PRD v1.3 §4.2-4.5 章节分类。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum MessageCategory {
    /** 实时类业务报文 — PRD §4.2 (4 个) */
    REALTIME,
    /** 非实时类业务报文 — PRD §4.3 (8 个) */
    BATCH,
    /** 供应链数字融资平台报文 — PRD §4.4 (23 个) */
    SUPPLY_CHAIN,
    /** 通用报文 — PRD §4.5 (9 个) */
    COMMON
}
