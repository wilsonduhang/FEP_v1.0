package com.puchain.fep.processor.routing;

/**
 * PRD §4.6 单条方向映射：方向 + 是否需 FEP + 处理模式。
 *
 * <p>角色由 {@code (MessageType, AccessRole)} 组合 Key 提供，本 record 不重复包含。</p>
 *
 * @param direction   角色视角方向
 * @param requiresFep 该角色该报文是否需要经过 FEP 路由
 * @param mode        PRD §4.7 处理模式
 * @author FEP Team
 * @since 1.0.0
 */
public record DirectionMapping(
        RoleDirection direction,
        boolean requiresFep,
        ProcessingMode mode
) { }
