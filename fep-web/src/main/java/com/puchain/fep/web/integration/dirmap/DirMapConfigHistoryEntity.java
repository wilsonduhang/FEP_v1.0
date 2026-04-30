package com.puchain.fep.web.integration.dirmap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * DIR-MAP 配置变更审计记录（FR-MSG-DIR-MAP-CONFIG，衍生）。
 *
 * <p>fep-web Adapter 层 JPA Entity（v1f D7 Hexagonal 决策 — fep-processor 不见 JPA）。
 * 每次 t_dir_map_config 写入产生 1 条 history 记录，保留 old/new 双方向数据
 * 用于回滚。changed_at 由调用层显式赋值（不依赖 DB now() 以保证测试可控）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_dir_map_config_history")
public class DirMapConfigHistoryEntity {

    @Id
    @Column(name = "history_id", nullable = false, length = 32)
    private String historyId;

    @Column(name = "message_type", nullable = false, length = 4)
    private String messageType;

    @Column(name = "access_role", nullable = false, length = 32)
    private String accessRole;

    @Column(name = "old_direction", length = 32)
    private String oldDirection;

    @Column(name = "old_requires_fep")
    private Boolean oldRequiresFep;

    @Column(name = "old_mode", length = 16)
    private String oldMode;

    @Column(name = "new_direction", nullable = false, length = 32)
    private String newDirection;

    @Column(name = "new_requires_fep", nullable = false)
    private Boolean newRequiresFep;

    @Column(name = "new_mode", nullable = false, length = 16)
    private String newMode;

    @Column(name = "changed_by", nullable = false, length = 64)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(final String historyId) {
        this.historyId = historyId;
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

    public String getOldDirection() {
        return oldDirection;
    }

    public void setOldDirection(final String oldDirection) {
        this.oldDirection = oldDirection;
    }

    public Boolean getOldRequiresFep() {
        return oldRequiresFep;
    }

    public void setOldRequiresFep(final Boolean oldRequiresFep) {
        this.oldRequiresFep = oldRequiresFep;
    }

    public String getOldMode() {
        return oldMode;
    }

    public void setOldMode(final String oldMode) {
        this.oldMode = oldMode;
    }

    public String getNewDirection() {
        return newDirection;
    }

    public void setNewDirection(final String newDirection) {
        this.newDirection = newDirection;
    }

    public Boolean getNewRequiresFep() {
        return newRequiresFep;
    }

    public void setNewRequiresFep(final Boolean newRequiresFep) {
        this.newRequiresFep = newRequiresFep;
    }

    public String getNewMode() {
        return newMode;
    }

    public void setNewMode(final String newMode) {
        this.newMode = newMode;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(final String changedBy) {
        this.changedBy = changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(final Instant changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(final String changeReason) {
        this.changeReason = changeReason;
    }
}
