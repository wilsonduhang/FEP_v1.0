package com.puchain.fep.web.entquery.auth.domain;

/**
 * 授权书状态枚举。
 *
 * <p>生命周期: DRAFT -> SUBMITTED -> ACKNOWLEDGED / REJECTED</p>
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum LetterStatus {

    /** 草稿（初始状态，可编辑/删除/提交）。 */
    DRAFT,

    /** 已提交（等待平台确认）。 */
    SUBMITTED,

    /** 已确认（平台已接受授权书）。 */
    ACKNOWLEDGED,

    /** 已拒绝（平台拒绝授权书）。 */
    REJECTED
}
