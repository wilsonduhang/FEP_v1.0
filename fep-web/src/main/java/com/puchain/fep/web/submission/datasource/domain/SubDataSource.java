package com.puchain.fep.web.submission.datasource.domain;

import com.puchain.fep.common.domain.EnableDisableStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 数据源 Entity，映射 t_sub_data_source 表。
 *
 * <p>参见 PRD v1.3 §5.5.3 数据源管理（FR-WEB-SUB-SRC）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sub_data_source")
@EntityListeners(AuditingEntityListener.class)
public class SubDataSource {

    /** 数据源唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "source_id", length = 32)
    private String sourceId;

    /** 数据源名称。 */
    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    /** LOGO 文件路径。 */
    @Column(name = "logo_path", length = 500)
    private String logoPath;

    /** 联系地址。 */
    @Column(name = "contact_address", nullable = false, length = 200)
    private String contactAddress;

    /** 联系电话。 */
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    /** 推送开关。 */
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    /** Content-Type。 */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** Client-Id。 */
    @Column(name = "client_id", length = 100)
    private String clientId;

    /** 数据源状态（ENABLED / DISABLED）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_status", nullable = false, length = 20)
    private EnableDisableStatus sourceStatus;

    /** 创建时间。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 无参构造方法（JPA 要求）。 */
    public SubDataSource() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取数据源唯一标识。
     *
     * @return 数据源 ID (UUID 32位)
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
     * 获取 LOGO 文件路径（可为 null）。
     *
     * @return LOGO 文件路径
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
     * 获取 Content-Type（可为 null）。
     *
     * @return Content-Type
     */
    public String getContentType() {
        return contentType;
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

    // ===== Setters =====

    /**
     * 设置数据源唯一标识。
     *
     * @param sourceId 数据源 ID
     */
    public void setSourceId(final String sourceId) {
        this.sourceId = sourceId;
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
     * 设置 LOGO 文件路径。
     *
     * @param logoPath LOGO 文件路径（可为 null）
     */
    public void setLogoPath(final String logoPath) {
        this.logoPath = logoPath;
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
     * 设置联系电话。
     *
     * @param contactPhone 联系电话
     */
    public void setContactPhone(final String contactPhone) {
        this.contactPhone = contactPhone;
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
     * 设置 Content-Type。
     *
     * @param contentType Content-Type（可为 null）
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * 设置 Client-Id。
     *
     * @param clientId Client-Id（可为 null）
     */
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /**
     * 设置数据源状态。
     *
     * @param sourceStatus 数据源状态枚举
     */
    public void setSourceStatus(final EnableDisableStatus sourceStatus) {
        this.sourceStatus = sourceStatus;
    }
}
