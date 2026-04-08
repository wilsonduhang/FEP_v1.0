package com.puchain.fep.web.dashboard.shortcut.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 快捷入口创建请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.2.4 快捷入口。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ShortcutCreateRequest {

    /** 快捷入口名称（2-50 字符）。 */
    @NotBlank(message = "快捷入口名称不能为空")
    @Size(min = 2, max = 50, message = "快捷入口名称长度 2-50 字符")
    private String shortcutName;

    /** 目标跳转 URL。 */
    @NotBlank(message = "目标 URL 不能为空")
    private String targetUrl;

    /** 图标标识（可选）。 */
    private String icon;

    /** 排序序号（默认 0）。 */
    private int sortOrder;

    /**
     * 获取快捷入口名称。
     *
     * @return 快捷入口名称
     */
    public String getShortcutName() {
        return shortcutName;
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
     * 获取目标跳转 URL。
     *
     * @return 目标 URL
     */
    public String getTargetUrl() {
        return targetUrl;
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
     * 获取图标标识（可为 null）。
     *
     * @return 图标标识
     */
    public String getIcon() {
        return icon;
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
     * 获取排序序号。
     *
     * @return 排序序号
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置排序序号。
     *
     * @param sortOrder 排序序号
     */
    public void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
