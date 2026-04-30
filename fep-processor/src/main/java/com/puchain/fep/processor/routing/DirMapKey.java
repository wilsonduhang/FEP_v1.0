package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;

import java.util.Objects;

/**
 * 报文方向映射的复合主键（msgType + accessRole）。
 *
 * <p>用于 {@link DynamicMessageDirectionMap} 内部 Caffeine cache 的 key、
 * {@link DirMapConfigStore} Port 接口的查询参数承载，以及 fep-web Adapter
 * 与 Service 层的 DTO 转换中转。声明为 record 保证 equals / hashCode 自动生成、
 * 不可变、线程安全。</p>
 *
 * @param msg  报文类型，非 null
 * @param role 接入角色，非 null
 * @author FEP Team
 * @since 1.0.0
 */
public record DirMapKey(MessageType msg, AccessRole role) {

    /**
     * Compact constructor 校验 null 参数。
     */
    public DirMapKey {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(role, "role");
    }
}
