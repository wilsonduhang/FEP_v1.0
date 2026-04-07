package com.puchain.fep.web.entquery.task.domain;

/**
 * 查询任务状态枚举。
 *
 * <p>生命周期: DRAFT -> PROCESSING -> COMPLETED / FAILED</p>
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum QueryTaskStatus {

    /** 草稿（初始状态，可编辑/删除/执行）。 */
    DRAFT,

    /** 处理中（已提交执行，等待 TLQ 回执）。 */
    PROCESSING,

    /** 已完成（查询成功并收到结果）。 */
    COMPLETED,

    /** 失败（查询失败或超时）。 */
    FAILED
}
