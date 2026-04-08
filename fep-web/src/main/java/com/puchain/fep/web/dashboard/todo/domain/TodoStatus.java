package com.puchain.fep.web.dashboard.todo.domain;

/**
 * 待办事项状态枚举。
 *
 * <p>参见 PRD v1.3 §5.2.2 待办事项区域（FR-WEB-DASH-TODO）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TodoStatus {

    /** 待处理。 */
    PENDING,

    /** 处理中。 */
    IN_PROCESS,

    /** 已完成。 */
    COMPLETED
}
