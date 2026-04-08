package com.puchain.fep.web.dashboard.todo.domain;

/**
 * 待办事项优先级枚举。
 *
 * <p>参见 PRD v1.3 §5.2.2 待办事项区域（FR-WEB-DASH-TODO）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TodoPriority {

    /** 紧急。 */
    URGENT,

    /** 高优先级。 */
    HIGH,

    /** 中优先级。 */
    MEDIUM,

    /** 低优先级。 */
    LOW
}
