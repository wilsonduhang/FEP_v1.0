package com.puchain.fep.web.sysmgmt.help.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 帮助内容创建请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.8 上下文帮助面板。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class HelpCreateRequest {

    @NotBlank(message = "页面编码不能为空")
    @Size(max = 50, message = "页面编码长度不能超过 50 个字符")
    private String pageCode;

    @NotBlank(message = "帮助标题不能为空")
    @Size(max = 200, message = "帮助标题长度不能超过 200 个字符")
    private String title;

    @NotBlank(message = "简要描述不能为空")
    @Size(max = 500, message = "简要描述长度不能超过 500 个字符")
    private String summary;

    @NotBlank(message = "详细内容不能为空")
    private String content;

    // ===== Getters =====

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

    // ===== Setters =====

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
}
