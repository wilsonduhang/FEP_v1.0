package com.puchain.fep.web.tlq.node.dto;

import com.puchain.fep.web.tlq.node.domain.TlqNode;
import com.puchain.fep.web.tlq.node.domain.TlqNodeRole;
import com.puchain.fep.web.tlq.node.domain.TlqNodeStatus;

import java.time.LocalDateTime;

/**
 * TLQ 节点配置响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TlqNodeResponse {

    private String nodeId;
    private String nodeName;
    private TlqNodeRole nodeRole;
    private String hostIp;
    private int port;
    private String vipAddress;
    private String protocol;
    private TlqNodeStatus nodeStatus;
    private String description;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 无参构造方法。 */
    public TlqNodeResponse() { /* for serialization */ }

    /**
     * 从 TlqNode Entity 构建响应 DTO。
     *
     * @param entity TlqNode 实体
     * @return 响应 DTO
     */
    public static TlqNodeResponse fromEntity(final TlqNode entity) {
        TlqNodeResponse resp = new TlqNodeResponse();
        resp.nodeId = entity.getNodeId();
        resp.nodeName = entity.getNodeName();
        resp.nodeRole = entity.getNodeRole();
        resp.hostIp = entity.getHostIp();
        resp.port = entity.getPort();
        resp.vipAddress = entity.getVipAddress();
        resp.protocol = entity.getProtocol();
        resp.nodeStatus = entity.getNodeStatus();
        resp.description = entity.getDescription();
        resp.lastHeartbeat = entity.getLastHeartbeat();
        resp.createTime = entity.getCreateTime();
        resp.updateTime = entity.getUpdateTime();
        return resp;
    }

    /**
     * 获取节点 ID。
     *
     * @return 节点 ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 获取节点名称。
     *
     * @return 节点名称
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * 获取节点角色。
     *
     * @return 节点角色
     */
    public TlqNodeRole getNodeRole() {
        return nodeRole;
    }

    /**
     * 获取主机 IP。
     *
     * @return 主机 IP
     */
    public String getHostIp() {
        return hostIp;
    }

    /**
     * 获取监听端口。
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取 VIP 地址。
     *
     * @return VIP 地址（可为 null）
     */
    public String getVipAddress() {
        return vipAddress;
    }

    /**
     * 获取通信协议。
     *
     * @return 协议
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * 获取节点状态。
     *
     * @return 节点状态
     */
    public TlqNodeStatus getNodeStatus() {
        return nodeStatus;
    }

    /**
     * 获取节点描述。
     *
     * @return 描述（可为 null）
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取最近心跳时间。
     *
     * @return 最近心跳时间（可为 null）
     */
    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
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
