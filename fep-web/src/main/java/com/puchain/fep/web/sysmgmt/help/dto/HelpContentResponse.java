package com.puchain.fep.web.sysmgmt.help.dto;

import com.puchain.fep.web.sysmgmt.help.domain.SysHelpContent;

/**
 * 帮助内容响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.8 上下文帮助面板。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class HelpContentResponse {

    private String helpId;
    private String pageCode;
    private String title;
    private String summary;
    private String content;
    private Integer sortOrder;

    /**
     * 从 {@link SysHelpContent} Entity 构建响应 DTO。
     *
     * @param entity 帮助内容 Entity
     * @return 帮助内容响应 DTO
     */
    public static HelpContentResponse from(final SysHelpContent entity) {
        HelpContentResponse resp = new HelpContentResponse();
        resp.setHelpId(entity.getHelpId());
        resp.setPageCode(entity.getPageCode());
        resp.setTitle(entity.getTitle());
        resp.setSummary(entity.getSummary());
        resp.setContent(entity.getContent());
        resp.setSortOrder(entity.getSortOrder());
        return resp;
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
     * 获取排序值。
     *
     * @return 排序值（值越小越靠前）
     */
    public Integer getSortOrder() {
        return sortOrder;
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
}
