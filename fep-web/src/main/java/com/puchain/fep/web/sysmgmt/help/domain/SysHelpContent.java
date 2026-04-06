package com.puchain.fep.web.sysmgmt.help.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 帮助面板内容 Entity，映射 t_sys_help_content 表。
 *
 * <p>每个页面（由 pageCode 标识）可配置最多 4 条帮助内容，
 * 通过 helpStatus 控制显示/禁用，sortOrder 控制显示顺序。
 * 参见 PRD v1.3 §5.10.8 上下文帮助面板。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_help_content")
public class SysHelpContent {

    @Id
    @Column(name = "help_id", length = 32)
    private String helpId;

    @Column(name = "page_code", nullable = false, length = 50)
    private String pageCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "help_status", nullable = false, length = 20)
    private String helpStatus;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysHelpContent() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取帮助唯一标识。
     *
     * @return 帮助 ID (UUID 32位)
     */
    public String getHelpId() {
        return helpId;
    }

    /**
     * 获取页面编码。
     *
     * @return 页面编码（如 sys-user、sys-role）
     */
    public String getPageCode() {
        return pageCode;
    }

    /**
     * 获取帮助标题。
     *
     * @return 帮助标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取简要描述。
     *
     * @return 简要描述
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 获取详细内容。
     *
     * @return 详细内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取排序值（值越小越靠前）。
     *
     * @return 排序值
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 获取帮助状态。
     *
     * @return 帮助状态（ACTIVE/DISABLED）
     */
    public String getHelpStatus() {
        return helpStatus;
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
     * 设置帮助唯一标识。
     *
     * @param helpId 帮助 ID (UUID 32位)
     */
    public void setHelpId(String helpId) {
        this.helpId = helpId;
    }

    /**
     * 设置页面编码。
     *
     * @param pageCode 页面编码（如 sys-user、sys-role）
     */
    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    /**
     * 设置帮助标题。
     *
     * @param title 帮助标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 设置简要描述。
     *
     * @param summary 简要描述
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 设置详细内容。
     *
     * @param content 详细内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 设置排序值。
     *
     * @param sortOrder 排序值（值越小越靠前）
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 设置帮助状态。
     *
     * @param helpStatus 帮助状态（ACTIVE/DISABLED）
     */
    public void setHelpStatus(String helpStatus) {
        this.helpStatus = helpStatus;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
