package com.puchain.fep.web.sysmgmt.config.enterprise.dto;

import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterpriseBiz;

import java.time.LocalDateTime;

/**
 * 企业业务信息关联响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.3 企业主体管理（Tab 2 业务信息）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class EnterpriseBizInfoResponse {

    private String id;
    private String enterpriseId;
    private String businessTypeId;
    private String configJson;
    private String status;
    private LocalDateTime createTime;

    /**
     * 从 Entity 构造响应 DTO。
     *
     * @param entity 企业业务信息关联 Entity
     * @return 响应 DTO
     */
    public static EnterpriseBizInfoResponse from(final SysEnterpriseBiz entity) {
        EnterpriseBizInfoResponse resp = new EnterpriseBizInfoResponse();
        resp.id = entity.getId();
        resp.enterpriseId = entity.getEnterpriseId();
        resp.businessTypeId = entity.getBusinessTypeId();
        resp.configJson = entity.getConfigJson();
        resp.status = entity.getStatus();
        resp.createTime = entity.getCreateTime();
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
     * 获取业务类型 ID。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * 获取配置 JSON。
     *
     * @return 配置 JSON（可为 null）
     */
    public String getConfigJson() {
        return configJson;
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
}
