package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;

import java.util.Objects;
import java.util.Optional;

/**
 * Port-backed direction map placeholder (P3a T4 stub — full implementation in T3 commit).
 *
 * <p><b>本类在 P3a T4 commit 1 阶段为占位骨架</b>：仅持有 {@link #lookupRaw} 方法，
 * 永远返回 {@link Optional#empty()}，使得 {@link MessageDirectionMap#lookup} 通过
 * {@link MessageDirectionMapBridge} 调用本类时无条件落到静态 fallback。</p>
 *
 * <p>P3a T3 commit 2 将本类替换为完整 Hexagonal Port + Caffeine cache 实现，
 * 引入 {@link DirMapConfigStore} 依赖、{@code @Component @DependsOn("messageDirectionMapBridge")}
 * 注解、{@code @PostConstruct} 启动期 cache 加载、{@code reload()} atomic putAll，
 * 以及 {@link MessageDirectionMapBridge#setDynamic(DynamicMessageDirectionMap)} 显式注入。</p>
 *
 * <p><b>禁止</b>在本占位文件直接消费此类（生产路径在 T3 替换前不会激活 — Bridge.dynamic
 * 字段为 null 时 {@link MessageDirectionMap#lookup} 短路）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DynamicMessageDirectionMap {

    /**
     * Cache + Port 二级查询占位。T4 commit 1 阶段总是返回 empty，使
     * {@link MessageDirectionMap#lookup} 落到 {@link MessageDirectionMap#staticLookup} fallback。
     *
     * @param msg  报文类型，非 null
     * @param role 接入角色，非 null
     * @return P3a T4 commit 1 阶段恒为 {@link Optional#empty()}（T3 commit 2 替换为真实查询）
     */
    public Optional<DirectionMapping> lookupRaw(final MessageType msg, final AccessRole role) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(role, "role");
        return Optional.empty();
    }
}
