package com.puchain.fep.web.integration.dirmap;

import java.io.Serializable;
import java.util.Objects;

/**
 * fep-web Adapter 层 JPA 复合主键 — {@code (message_type, access_role)} 字符串对。
 *
 * <p>v1g P0-B 修复（Round 4 F P0-1）— 与 fep-processor.routing.DirMapKey 解耦：
 * Port 接口 DirMapKey 用 MessageType / AccessRole 枚举；JPA 这一侧持字符串
 * 便于 schema 与 V19 列定义对齐。Entity → Snapshot 转换在 Adapter 内做。</p>
 *
 * <p>用 {@code @IdClass} 复合 PK 模式（implements Serializable），<b>非</b>
 * {@code @Embeddable}/{@code @EmbeddedId} 模式 — Plan §4 line 877 + 1164。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DirMapConfigKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageType;
    private String accessRole;

    /** No-arg constructor required by JPA / @IdClass. */
    public DirMapConfigKey() {
    }

    /**
     * 全参构造函数。
     *
     * @param messageType 报文类型字符串（4 位 HNDEMP 报文号），非 null
     * @param accessRole  接入角色枚举名（{@code ACCEPTING_ORG} / {@code INFO_SERVICE_ORG}），非 null
     */
    public DirMapConfigKey(final String messageType, final String accessRole) {
        this.messageType = messageType;
        this.accessRole = accessRole;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    public String getAccessRole() {
        return accessRole;
    }

    public void setAccessRole(final String accessRole) {
        this.accessRole = accessRole;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DirMapConfigKey other)) {
            return false;
        }
        return Objects.equals(messageType, other.messageType)
                && Objects.equals(accessRole, other.accessRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType, accessRole);
    }
}
