package com.puchain.fep.web.submission.datasource.dto;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.datasource.domain.SubDataSource;

import java.time.LocalDateTime;

/**
 * 数据源响应 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DataSourceResponse {

    /** 数据源 ID。 */
    private String sourceId;

    /** 数据源名称。 */
    private String sourceName;

    /** LOGO 文件路径。 */
    private String logoPath;

    /** 联系地址。 */
    private String contactAddress;

    /** 联系电话。 */
    private String contactPhone;

    /** 推送开关。 */
    private boolean pushEnabled;

    /** Content-Type。 */
    private String contentType;

    /** Client-Id。 */
    private String clientId;

    /** 数据源状态。 */
    private EnableDisableStatus sourceStatus;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 数据源 Entity
     * @return 响应 DTO
     */
    public static DataSourceResponse from(final SubDataSource entity) {
        DataSourceResponse resp = new DataSourceResponse();
        resp.sourceId = entity.getSourceId();
        resp.sourceName = entity.getSourceName();
        resp.logoPath = entity.getLogoPath();
        resp.contactAddress = entity.getContactAddress();
        resp.contactPhone = entity.getContactPhone();
        resp.pushEnabled = entity.isPushEnabled();
        resp.contentType = entity.getContentType();
        resp.clientId = entity.getClientId();
        resp.sourceStatus = entity.getSourceStatus();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取数据源 ID。
     *
     * @return 数据源 ID
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * 获取数据源名称。
     *
     * @return 数据源名称
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * 获取 LOGO 文件路径。
     *
     * @return LOGO 文件路径（可为 null）
     */
    public String getLogoPath() {
        return logoPath;
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
     * 获取联系电话。
     *
     * @return 联系电话
     */
    public String getContactPhone() {
        return contactPhone;
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
     * 获取 Content-Type。
     *
     * @return Content-Type（可为 null）
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 获取 Client-Id。
     *
     * @return Client-Id（可为 null）
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 获取数据源状态。
     *
     * @return 数据源状态枚举
     */
    public EnableDisableStatus getSourceStatus() {
        return sourceStatus;
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
