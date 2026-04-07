package com.puchain.fep.web.sysmgmt.config.enterprise.dto;

import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterpriseQueryConfig;

import java.time.LocalDateTime;

/**
 * 企业精准查询配置响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.3 企业主体管理（Tab 3 精准查询配置）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class EnterpriseQueryConfigResponse {

    private String id;
    private String enterpriseId;
    private String queryType;
    private String queryParams;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 企业精准查询配置 Entity
     * @return 响应 DTO
     */
    public static EnterpriseQueryConfigResponse from(final SysEnterpriseQueryConfig entity) {
        EnterpriseQueryConfigResponse resp = new EnterpriseQueryConfigResponse();
        resp.id = entity.getId();
        resp.enterpriseId = entity.getEnterpriseId();
        resp.queryType = entity.getQueryType();
        resp.queryParams = entity.getQueryParams();
        resp.status = entity.getStatus();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取记录 ID。
     *
     * @return 记录 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取企业主体 ID。
     *
     * @return 企业主体 ID
     */
    public String getEnterpriseId() {
        return enterpriseId;
    }

    /**
     * 获取查询类型。
     *
     * @return 查询类型
     */
    public String getQueryType() {
        return queryType;
    }

    /**
     * 获取查询参数 JSON。
     *
     * @return 查询参数（可为 null）
     */
    public String getQueryParams() {
        return queryParams;
    }

    /**
     * 获取状态。
     *
     * @return 状态
     */
    public String getStatus() {
        return status;
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
