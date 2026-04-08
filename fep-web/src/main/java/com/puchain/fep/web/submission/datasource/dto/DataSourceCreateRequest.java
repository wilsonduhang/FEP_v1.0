package com.puchain.fep.web.submission.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 数据源创建/编辑请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.5.3 数据源管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DataSourceCreateRequest {

    /** 数据源名称。 */
    @NotBlank(message = "数据源名称不能为空")
    @Size(min = 1, max = 30, message = "数据源名称长度 1-30 字符")
    private String sourceName;

    /** LOGO 文件路径（可选）。 */
    private String logoPath;

    /** 联系地址。 */
    @NotBlank(message = "联系地址不能为空")
    @Size(min = 1, max = 50, message = "联系地址长度 1-50 字符")
    private String contactAddress;

    /** 联系电话（1-11 位数字）。 */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^\\d{1,11}$", message = "联系电话为 1-11 位数字")
    private String contactPhone;

    /** 推送开关（可选，默认 false）。 */
    private boolean pushEnabled;

    /** Content-Type（可选）。 */
    private String contentType;

    /** Client-Id（可选）。 */
    private String clientId;

    /**
     * 获取数据源名称。
     *
     * @return 数据源名称
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * 设置数据源名称。
     *
     * @param sourceName 数据源名称
     */
    public void setSourceName(final String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * 获取 LOGO 文件路径（可为 null）。
     *
     * @return LOGO 文件路径
     */
    public String getLogoPath() {
        return logoPath;
    }

    /**
     * 设置 LOGO 文件路径。
     *
     * @param logoPath LOGO 文件路径（可为 null）
     */
    public void setLogoPath(final String logoPath) {
        this.logoPath = logoPath;
    }

    /**
     * 获取联系地址。
     *
     * @return 联系地址
     */
    public String getContactAddress() {
        return contactAddress;
    }

    /**
     * 设置联系地址。
     *
     * @param contactAddress 联系地址
     */
    public void setContactAddress(final String contactAddress) {
        this.contactAddress = contactAddress;
    }

    /**
     * 获取联系电话。
     *
     * @return 联系电话
     */
    public String getContactPhone() {
        return contactPhone;
    }

    /**
     * 设置联系电话。
     *
     * @param contactPhone 联系电话
     */
    public void setContactPhone(final String contactPhone) {
        this.contactPhone = contactPhone;
    }

    /**
     * 获取推送开关状态。
     *
     * @return true 表示已启用推送
     */
    public boolean isPushEnabled() {
        return pushEnabled;
    }

    /**
     * 设置推送开关。
     *
     * @param pushEnabled 推送开关
     */
    public void setPushEnabled(final boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    /**
     * 获取 Content-Type（可为 null）。
     *
     * @return Content-Type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 设置 Content-Type。
     *
     * @param contentType Content-Type（可为 null）
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取 Client-Id（可为 null）。
     *
     * @return Client-Id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 设置 Client-Id。
     *
     * @param clientId Client-Id（可为 null）
     */
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }
}
