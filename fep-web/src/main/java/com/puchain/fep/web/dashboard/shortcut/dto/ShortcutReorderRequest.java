package com.puchain.fep.web.dashboard.shortcut.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 快捷入口重排序请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.2.4 快捷入口。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ShortcutReorderRequest {

    /** 重排序列表。 */
    @NotEmpty(message = "重排序列表不能为空")
    @Valid
    private List<ReorderItem> items;

    /**
     * 获取重排序列表。
     *
     * @return 重排序列表
     */
    public List<ReorderItem> getItems() {
        return items;
    }

    /**
     * 设置重排序列表。
     *
     * @param items 重排序列表
     */
    public void setItems(final List<ReorderItem> items) {
        this.items = items;
    }

    /**
     * 重排序项，包含快捷入口 ID 和新的排序序号。
     */
    public static class ReorderItem {

        /** 快捷入口 ID。 */
        @NotNull(message = "快捷入口 ID 不能为空")
        private String shortcutId;

        /** 新的排序序号。 */
        @NotNull(message = "排序序号不能为空")
        private Integer sortOrder;

        /**
         * 获取快捷入口 ID。
         *
         * @return 快捷入口 ID
         */
        public String getShortcutId() {
            return shortcutId;
        }

        /**
         * 设置快捷入口 ID。
         *
         * @param shortcutId 快捷入口 ID
         */
        public void setShortcutId(final String shortcutId) {
            this.shortcutId = shortcutId;
        }

        /**
         * 获取排序序号。
         *
         * @return 排序序号
         */
        public Integer getSortOrder() {
            return sortOrder;
        }

        /**
         * 设置排序序号。
         *
         * @param sortOrder 排序序号
         */
        public void setSortOrder(final Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
