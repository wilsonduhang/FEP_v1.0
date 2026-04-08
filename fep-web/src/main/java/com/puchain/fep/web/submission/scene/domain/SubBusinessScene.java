package com.puchain.fep.web.submission.scene.domain;

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
 * 业务场景 Entity，映射 t_sub_business_scene 表。
 *
 * <p>参见 PRD v1.3 §5.5.4 业务场景管理（FR-WEB-SUB-SCENE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sub_business_scene")
@EntityListeners(AuditingEntityListener.class)
public class SubBusinessScene {

    /** 场景唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "scene_id", length = 32)
    private String sceneId;

    /** 场景名称。 */
    @Column(name = "scene_name", nullable = false, length = 100)
    private String sceneName;

    /** 关联业务类型 ID。 */
    @Column(name = "business_type_id", nullable = false, length = 32)
    private String businessTypeId;

    /** 推送方式。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "push_method", nullable = false, length = 20)
    private ScenePushMethod pushMethod;

    /** 导入模板文件路径（MANUAL 模式必填）。 */
    @Column(name = "import_template_path", length = 500)
    private String importTemplatePath;

    /** 场景数据获取地址。 */
    @Column(name = "request_url", nullable = false, length = 500)
    private String requestUrl;

    /** 排序数值。 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 场景状态（ENABLED / DISABLED）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "scene_status", nullable = false, length = 20)
    private EnableDisableStatus sceneStatus;

    /** 创建时间。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 无参构造方法（JPA 要求）。 */
    public SubBusinessScene() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取场景唯一标识。
     *
     * @return 场景 ID (UUID 32位)
     */
    public String getSceneId() {
        return sceneId;
    }

    /**
     * 获取场景名称。
     *
     * @return 场景名称
     */
    public String getSceneName() {
        return sceneName;
    }

    /**
     * 获取关联业务类型 ID。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * 获取推送方式。
     *
     * @return 推送方式枚举
     */
    public ScenePushMethod getPushMethod() {
        return pushMethod;
    }

    /**
     * 获取导入模板文件路径（可为 null）。
     *
     * @return 导入模板路径
     */
    public String getImportTemplatePath() {
        return importTemplatePath;
    }

    /**
     * 获取场景数据获取地址。
     *
     * @return 请求 URL
     */
    public String getRequestUrl() {
        return requestUrl;
    }

    /**
     * 获取排序数值。
     *
     * @return 排序数值
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * 获取场景状态。
     *
     * @return 场景状态枚举
     */
    public EnableDisableStatus getSceneStatus() {
        return sceneStatus;
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
     * 设置场景唯一标识。
     *
     * @param sceneId 场景 ID
     */
    public void setSceneId(final String sceneId) {
        this.sceneId = sceneId;
    }

    /**
     * 设置场景名称。
     *
     * @param sceneName 场景名称
     */
    public void setSceneName(final String sceneName) {
        this.sceneName = sceneName;
    }

    /**
     * 设置关联业务类型 ID。
     *
     * @param businessTypeId 业务类型 ID
     */
    public void setBusinessTypeId(final String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    /**
     * 设置推送方式。
     *
     * @param pushMethod 推送方式枚举
     */
    public void setPushMethod(final ScenePushMethod pushMethod) {
        this.pushMethod = pushMethod;
    }

    /**
     * 设置导入模板文件路径。
     *
     * @param importTemplatePath 导入模板路径（可为 null）
     */
    public void setImportTemplatePath(final String importTemplatePath) {
        this.importTemplatePath = importTemplatePath;
    }

    /**
     * 设置场景数据获取地址。
     *
     * @param requestUrl 请求 URL
     */
    public void setRequestUrl(final String requestUrl) {
        this.requestUrl = requestUrl;
    }

    /**
     * 设置排序数值。
     *
     * @param sortOrder 排序数值
     */
    public void setSortOrder(final int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 设置场景状态。
     *
     * @param sceneStatus 场景状态枚举
     */
    public void setSceneStatus(final EnableDisableStatus sceneStatus) {
        this.sceneStatus = sceneStatus;
    }
}
