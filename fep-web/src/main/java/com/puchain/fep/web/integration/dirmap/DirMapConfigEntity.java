package com.puchain.fep.web.integration.dirmap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * fep-web Adapter 层 JPA Entity — DIR-MAP 主配置（V19 schema 表 t_dir_map_config）。
 *
 * <p>v1g P0-B 修复：v1f 漏 Create 此类，导致 production Spring 找不到 @Primary Port 实现 →
 * fallback 到 InMemoryDirMapConfigStore default → 数据不持久化。</p>
 *
 * <p>D7 Hexagonal — Entity 仅在 fep-web；fep-processor 端 DirMapConfigStore Port 通过
 * JpaDirMapConfigStore Adapter 间接读写，不直接见此类。</p>
 *
 * <p>{@code requires_fep} 用 {@code boolean} 原始类型（NOT {@code Boolean} 包装），
 * 避免 unboxing NPE — Round 2 C P1-2 教训。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_dir_map_config")
@IdClass(DirMapConfigKey.class)
public class DirMapConfigEntity {

    @Id
    @Column(name = "message_type", nullable = false, length = 16)
    private String messageType;

    @Id
    @Column(name = "access_role", nullable = false, length = 32)
    private String accessRole;

    @Column(name = "direction", nullable = false, length = 32)
    private String direction;

    @Column(name = "requires_fep", nullable = false)
    private boolean requiresFep;

    @Column(name = "processing_mode", nullable = false, length = 16)
    private String processingMode;

    @Column(name = "updated_by", nullable = false, length = 64)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public String getDirection() {
        return direction;
    }

    public void setDirection(final String direction) {
        this.direction = direction;
    }

    public boolean isRequiresFep() {
        return requiresFep;
    }

    public void setRequiresFep(final boolean requiresFep) {
        this.requiresFep = requiresFep;
    }

    public String getProcessingMode() {
        return processingMode;
    }

    public void setProcessingMode(final String processingMode) {
        this.processingMode = processingMode;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
