package com.puchain.fep.web.submission.scene.dto;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.scene.domain.ScenePushMethod;
import com.puchain.fep.web.submission.scene.domain.SubBusinessScene;

import java.time.LocalDateTime;

/**
 * 业务场景响应 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class SceneResponse {

    /** 场景 ID。 */
    private String sceneId;

    /** 场景名称。 */
    private String sceneName;

    /** 关联业务类型 ID。 */
    private String businessTypeId;

    /** 推送方式。 */
    private ScenePushMethod pushMethod;

    /** 导入模板文件路径。 */
    private String importTemplatePath;

    /** 场景数据获取地址。 */
    private String requestUrl;

    /** 排序数值。 */
    private int sortOrder;

    /** 场景状态。 */
    private EnableDisableStatus sceneStatus;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 业务场景 Entity
     * @return 响应 DTO
     */
    public static SceneResponse from(final SubBusinessScene entity) {
        SceneResponse resp = new SceneResponse();
        resp.sceneId = entity.getSceneId();
        resp.sceneName = entity.getSceneName();
        resp.businessTypeId = entity.getBusinessTypeId();
        resp.pushMethod = entity.getPushMethod();
        resp.importTemplatePath = entity.getImportTemplatePath();
        resp.requestUrl = entity.getRequestUrl();
        resp.sortOrder = entity.getSortOrder();
        resp.sceneStatus = entity.getSceneStatus();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取场景 ID。
     *
     * @return 场景 ID
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
     * 获取导入模板文件路径。
     *
     * @return 导入模板路径（可为 null）
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
}
