package com.puchain.fep.web.tlq.queue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * TLQ 队列批量生成请求 DTO。
 *
 * <p>根据 PRD v1.3 §3.1.2 队列命名规范，按机构代码批量生成 9 条标准队列配置。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TlqQueueBatchGenerateRequest {

    /** 所属节点 ID。 */
    @NotBlank(message = "节点 ID 不能为空")
    private String nodeId;

    /** 机构代码（统一社会信用代码或机构编码，用于队列命名）。 */
    @NotBlank(message = "机构代码不能为空")
    @Size(min = 1, max = 50, message = "机构代码长度 1-50 字符")
    private String organizationCode;

    /**
     * 获取所属节点 ID。
     *
     * @return 节点 ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 设置所属节点 ID。
     *
     * @param nodeId 节点 ID
     */
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 获取机构代码。
     *
     * @return 机构代码
     */
    public String getOrganizationCode() {
        return organizationCode;
    }

    /**
     * 设置机构代码。
     *
     * @param organizationCode 机构代码
     */
    public void setOrganizationCode(final String organizationCode) {
        this.organizationCode = organizationCode;
    }
}
