package com.puchain.fep.web.entquery.task.domain;

/**
 * 企业信息查询类型枚举。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum QueryType {

    /** 实时查询。 */
    REALTIME,

    /** 批量查询。 */
    BATCH
}
