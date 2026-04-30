package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;

import java.time.Instant;

/**
 * 报文方向映射配置快照（POJO record，非 JPA Entity）。
 *
 * <p>{@code requiresFep} 用 {@code boolean} 原始类型而非 {@code Boolean} 包装类，
 * 避免下游 {@link DirectionMapping} 构造时 unboxing NullPointerException
 * （Round 2 C P1-2 修复）。Adapter 层 Entity → Snapshot 转换时若 DB 列允许 null
 * 必须显式默认值，<b>禁止</b>直接传 {@code Boolean} 包装类。</p>
 *
 * @param messageType 报文类型，非 null
 * @param accessRole  接入角色，非 null
 * @param direction   角色视角方向，非 null
 * @param requiresFep 该角色该报文是否需要经过 FEP 路由（原始类型）
 * @param mode        PRD §4.7 处理模式，非 null
 * @param updatedBy   最近一次更新人，非 null
 * @param updatedAt   最近一次更新时间，非 null
 * @author FEP Team
 * @since 1.0.0
 */
public record DirMapConfigSnapshot(
        MessageType messageType,
        AccessRole accessRole,
        RoleDirection direction,
        boolean requiresFep,
        ProcessingMode mode,
        String updatedBy,
        Instant updatedAt
) { }
