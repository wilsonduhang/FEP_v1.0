package com.puchain.fep.processor.routing;

/**
 * FEP 接入方角色（FR-MSG-DIR-MAP 方向决策的一个维度）。
 *
 * <p>PRD §2.3 定义两种接入机构：受理单位（银行/保理/担保）和
 * 供应链信息服务机构（普链等）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum AccessRole {
    /** 受理单位（银行/保理/担保公司）。 */
    ACCEPTING_ORG,
    /** 供应链信息服务机构（如普链）。 */
    INFO_SERVICE_ORG
}
