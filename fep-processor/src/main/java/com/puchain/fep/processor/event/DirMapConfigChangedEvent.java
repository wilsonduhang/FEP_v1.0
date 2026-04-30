package com.puchain.fep.processor.event;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.RoleDirection;
import org.springframework.context.ApplicationEvent;

import java.util.Objects;

/**
 * DIR-MAP 配置变更事件。由 fep-web {@code DirMapConfigAdminService} 在
 * 单条 update 提交后发布；fep-processor 的 {@code DirMapCacheInvalidator}
 * 监听并触发 {@code DynamicMessageDirectionMap.reload()} 整体替换 cache。
 *
 * <p><b>Hexagonal 边界</b>：本类仅引用 fep-processor / fep-converter 包内类型，
 * <b>不</b>引用 fep-web 类，避免反向依赖。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class DirMapConfigChangedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final MessageType msgType;
    private final AccessRole role;
    private final RoleDirection oldDirection;
    private final RoleDirection newDirection;
    private final String updatedBy;

    /**
     * 构造方向变更事件。
     *
     * @param source       事件源（通常为发布者 service 实例），非 null
     * @param msgType      报文类型，非 null
     * @param role         接入角色，非 null
     * @param oldDirection 旧方向，非 null
     * @param newDirection 新方向，非 null
     * @param updatedBy    操作人 username，非 null
     */
    public DirMapConfigChangedEvent(final Object source,
                                    final MessageType msgType,
                                    final AccessRole role,
                                    final RoleDirection oldDirection,
                                    final RoleDirection newDirection,
                                    final String updatedBy) {
        super(source);
        this.msgType = Objects.requireNonNull(msgType, "msgType");
        this.role = Objects.requireNonNull(role, "role");
        this.oldDirection = Objects.requireNonNull(oldDirection, "oldDirection");
        this.newDirection = Objects.requireNonNull(newDirection, "newDirection");
        this.updatedBy = Objects.requireNonNull(updatedBy, "updatedBy");
    }

    /**
     * @return 报文类型
     */
    public MessageType getMsgType() {
        return msgType;
    }

    /**
     * @return 接入角色
     */
    public AccessRole getRole() {
        return role;
    }

    /**
     * @return 旧方向（变更前）
     */
    public RoleDirection getOldDirection() {
        return oldDirection;
    }

    /**
     * @return 新方向（变更后）
     */
    public RoleDirection getNewDirection() {
        return newDirection;
    }

    /**
     * @return 操作人 username
     */
    public String getUpdatedBy() {
        return updatedBy;
    }
}
