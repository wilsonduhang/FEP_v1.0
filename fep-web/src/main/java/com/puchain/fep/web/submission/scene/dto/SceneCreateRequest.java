package com.puchain.fep.web.submission.scene.dto;

import com.puchain.fep.web.submission.scene.domain.ScenePushMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * 业务场景创建/编辑请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.5.4 业务场景管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class SceneCreateRequest {

    /** 场景名称。 */
    @NotBlank(message = "场景名称不能为空")
    @Size(min = 3, max = 30, message = "场景名称长度 3-30 字符")
    private String sceneName;

    /** 关联业务类型 ID。 */
    @NotBlank(message = "业务类型不能为空")
    private String businessTypeId;

    /** 推送方式。 */
    @NotNull(message = "推送方式不能为空")
    private ScenePushMethod pushMethod;

    /** 导入模板文件路径（MANUAL 模式必填）。 */
    private String importTemplatePath;

    /** 场景数据获取地址（合法 URL）。 */
    @NotBlank(message = "请求地址不能为空")
    @URL(message = "请求地址必须是合法 URL")
    private String requestUrl;

    /** 排序数值。 */
    @NotNull(message = "排序数值不能为空")
    private Integer sortOrder;

    /**
     * 获取场景名称。
     *
     * @return 场景名称
     */
    public String getSceneName() {
        return sceneName;
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
     * 获取关联业务类型 ID。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
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
     * 获取推送方式。
     *
     * @return 推送方式枚举
     */
    public ScenePushMethod getPushMethod() {
        return pushMethod;
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
     * 获取导入模板文件路径（可为 null）。
     *
     * @return 导入模板路径
     */
    public String getImportTemplatePath() {
        return importTemplatePath;
    }

    /**
     * 设置导入模板文件路径。
     *
     * @param importTemplatePath 导入模板路径（MANUAL 模式必填）
     */
    public void setImportTemplatePath(final String importTemplatePath) {
        this.importTemplatePath = importTemplatePath;
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
     * 设置场景数据获取地址。
     *
     * @param requestUrl 请求 URL
     */
    public void setRequestUrl(final String requestUrl) {
        this.requestUrl = requestUrl;
    }

    /**
     * 获取排序数值。
     *
     * @return 排序数值
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置排序数值。
     *
     * @param sortOrder 排序数值
     */
    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
