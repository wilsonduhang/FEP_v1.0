package com.puchain.fep.web.sysmgmt.help.dto;

import jakarta.validation.constraints.Size;

/**
 * 帮助内容更新请求 DTO（局部更新，所有字段均可为 null）。
 *
 * <p>参见 PRD v1.3 §5.10.8 上下文帮助面板。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class HelpUpdateRequest {

    @Size(max = 200, message = "帮助标题长度不能超过 200 个字符")
    private String title;

    @Size(max = 500, message = "简要描述长度不能超过 500 个字符")
    private String summary;

    private String content;

    // ===== Getters =====

    /**
     * 获取帮助标题（可为 null，表示不更新）。
     *
     * @return 帮助标题，可能为 null
     */
    public String getTitle() {
        return title;
    }

    /**
     * 获取简要描述（可为 null，表示不更新）。
     *
     * @return 简要描述，可能为 null
     */
    public String getSummary() {
        return summary;
    }

    /**
     * 获取详细内容（可为 null，表示不更新）。
     *
     * @return 详细内容，可能为 null
     */
    public String getContent() {
        return content;
    }

    // ===== Setters =====

    /**
     * 设置帮助标题。
     *
     * @param title 帮助标题，传 null 表示不更新
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 设置简要描述。
     *
     * @param summary 简要描述，传 null 表示不更新
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 设置详细内容。
     *
     * @param content 详细内容，传 null 表示不更新
     */
    public void setContent(String content) {
        this.content = content;
    }
}
