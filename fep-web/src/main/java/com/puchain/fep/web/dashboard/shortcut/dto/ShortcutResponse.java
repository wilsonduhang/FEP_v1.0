package com.puchain.fep.web.dashboard.shortcut.dto;

import com.puchain.fep.web.dashboard.shortcut.domain.DashboardShortcut;

import java.time.LocalDateTime;

/**
 * 快捷入口响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.2.4 快捷入口。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ShortcutResponse {

    /** 快捷入口 ID。 */
    private String shortcutId;

    /** 所属用户 ID。 */
    private String userId;

    /** 快捷入口名称。 */
    private String shortcutName;

    /** 目标跳转 URL。 */
    private String targetUrl;

    /** 图标标识。 */
    private String icon;

    /** 排序序号。 */
    private int sortOrder;

    /** 是否可见。 */
    private Boolean visible;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 快捷入口 Entity
     * @return 响应 DTO
     */
    public static ShortcutResponse from(final DashboardShortcut entity) {
        ShortcutResponse resp = new ShortcutResponse();
        resp.shortcutId = entity.getShortcutId();
        resp.userId = entity.getUserId();
        resp.shortcutName = entity.getShortcutName();
        resp.targetUrl = entity.getTargetUrl();
        resp.icon = entity.getIcon();
        resp.sortOrder = entity.getSortOrder();
        resp.visible = entity.getVisible();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取快捷入口 ID。
     *
     * @return 快捷入口 ID
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
}
