package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;

import java.util.Objects;

/**
 * 单条方向映射更新参数。{@code messageType} + {@code accessRole} 定位主键，
 * 其余三字段（direction / requiresFep / mode）为可变维度。{@code updatedBy}
 * 由 fep-web Service 层从 SecurityContext 注入。
 *
 * @param messageType 报文类型，非 null
 * @param accessRole  接入角色，非 null
 * @param direction   新方向，非 null
 * @param requiresFep 新 requiresFep 值（原始类型）
 * @param mode        新处理模式，非 null
 * @param updatedBy   操作人 username，非 null
 * @author FEP Team
 * @since 1.0.0
 */
public record DirMapConfigUpdate(
        MessageType messageType,
        AccessRole accessRole,
        RoleDirection direction,
        boolean requiresFep,
        ProcessingMode mode,
        String updatedBy
) {

    /**
     * Compact constructor 校验所有引用字段非 null（{@code requiresFep} 为
     * 原始 {@code boolean} 自动有值，无需校验）。
     */
    public DirMapConfigUpdate {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(accessRole, "accessRole");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(updatedBy, "updatedBy");
    }
}
