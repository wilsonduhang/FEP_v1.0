package com.puchain.fep.web.dashboard.shortcut.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 首页快捷入口 Entity，映射 t_dashboard_shortcut 表。
 *
 * <p>参见 PRD v1.3 §5.2.4 快捷入口（FR-WEB-DASH-QUICK）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_dashboard_shortcut")
@EntityListeners(AuditingEntityListener.class)
public class DashboardShortcut {

    /** 快捷入口唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "shortcut_id", length = 32)
    private String shortcutId;

    /** 所属用户 ID。 */
    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    /** 快捷入口名称。 */
    @Column(name = "shortcut_name", nullable = false, length = 50)
    private String shortcutName;

    /** 目标跳转 URL。 */
    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    /** 图标标识（可为 null）。 */
    @Column(name = "icon", length = 100)
    private String icon;

    /** 排序序号。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 是否可见。 */
    @Column(name = "visible", nullable = false)
    private Boolean visible;

    /** 创建时间。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 无参构造方法（JPA 要求）。 */
    public DashboardShortcut() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取快捷入口唯一标识。
     *
     * @return 快捷入口 ID (UUID 32位)
     */
    public String getShortcutId() {
        return shortcutId;
    }

    /**
     * 获取所属用户 ID。
     *
     * @return 用户 ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 获取快捷入口名称。
     *
     * @return 快捷入口名称
     */
    public String getShortcutName() {
        return shortcutName;
    }

    /**
     * 获取目标跳转 URL。
     *
     * @return 目标 URL
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * 获取图标标识（可为 null）。
     *
     * @return 图标标识
     */
    public String getIcon() {
        return icon;
    }

    /**
     * 获取排序序号。
     *
     * @return 排序序号
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * 获取是否可见。
     *
     * @return 是否可见
     */
    public Boolean getVisible() {
        return visible;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== Setters =====

    /**
     * 设置快捷入口唯一标识。
     *
     * @param shortcutId 快捷入口 ID
     */
    public void setShortcutId(final String shortcutId) {
        this.shortcutId = shortcutId;
    }

    /**
     * 设置所属用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    /**
     * 设置快捷入口名称。
     *
     * @param shortcutName 快捷入口名称
     */
    public void setShortcutName(final String shortcutName) {
        this.shortcutName = shortcutName;
    }

    /**
     * 设置目标跳转 URL。
     *
     * @param targetUrl 目标 URL
     */
    public void setTargetUrl(final String targetUrl) {
        this.targetUrl = targetUrl;
    }

    /**
     * 设置图标标识。
     *
     * @param icon 图标标识（可为 null）
     */
    public void setIcon(final String icon) {
        this.icon = icon;
    }

    /**
     * 设置排序序号。
     *
     * @param sortOrder 排序序号
     */
    public void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 设置是否可见。
     *
     * @param visible 是否可见
     */
    public void setVisible(final Boolean visible) {
        this.visible = visible;
    }
}
