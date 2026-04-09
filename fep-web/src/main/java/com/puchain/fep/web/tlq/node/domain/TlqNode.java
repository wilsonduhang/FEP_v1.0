package com.puchain.fep.web.tlq.node.domain;

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
 * TLQ 节点配置 Entity，映射 t_tlq_node 表。
 *
 * <p>参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_tlq_node")
@EntityListeners(AuditingEntityListener.class)
public class TlqNode {

    /** 节点唯一标识（UUID 32位）。 */
    @Id
    @Column(name = "node_id", length = 32)
    private String nodeId;

    /** 节点名称（唯一）。 */
    @Column(name = "node_name", nullable = false, length = 100)
    private String nodeName;

    /** 节点角色。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "node_role", nullable = false, length = 30)
    private TlqNodeRole nodeRole;

    /** 主机 IP 地址。 */
    @Column(name = "host_ip", nullable = false, length = 50)
    private String hostIp;

    /** 监听端口。 */
    @Column(name = "port", nullable = false)
    private int port;

    /** VIP 地址（可为 null）。 */
    @Column(name = "vip_address", length = 50)
    private String vipAddress;

    /** 通信协议（默认 TCP）。 */
    @Column(name = "protocol", nullable = false, length = 20)
    private String protocol;

    /** 节点状态。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "node_status", nullable = false, length = 20)
    private TlqNodeStatus nodeStatus;

    /** 节点描述（可为 null）。 */
    @Column(name = "description", length = 500)
    private String description;

    /** 最近心跳时间（可为 null）。 */
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    /** 创建时间。 */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /** 无参构造方法（JPA 要求）。 */
    public TlqNode() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取节点唯一标识。
     *
     * @return 节点 ID (UUID 32位)
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
     * @return 节点角色枚举
     */
    public TlqNodeRole getNodeRole() {
        return nodeRole;
    }

    /**
     * 获取主机 IP。
     *
     * @return 主机 IP 地址
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
     * 获取 VIP 地址（可为 null）。
     *
     * @return VIP 地址
     */
    public String getVipAddress() {
        return vipAddress;
    }

    /**
     * 获取通信协议。
     *
     * @return 协议字符串
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * 获取节点状态。
     *
     * @return 节点状态枚举
     */
    public TlqNodeStatus getNodeStatus() {
        return nodeStatus;
    }

    /**
     * 获取节点描述（可为 null）。
     *
     * @return 描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取最近心跳时间（可为 null）。
     *
     * @return 最近心跳时间
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

    // ===== Setters =====

    /**
     * 设置节点唯一标识。
     *
     * @param nodeId 节点 ID
     */
    public void setNodeId(final String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 设置节点名称。
     *
     * @param nodeName 节点名称
     */
    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * 设置节点角色。
     *
     * @param nodeRole 节点角色枚举
     */
    public void setNodeRole(final TlqNodeRole nodeRole) {
        this.nodeRole = nodeRole;
    }

    /**
     * 设置主机 IP。
     *
     * @param hostIp 主机 IP 地址
     */
    public void setHostIp(final String hostIp) {
        this.hostIp = hostIp;
    }

    /**
     * 设置监听端口。
     *
     * @param port 端口号
     */
    public void setPort(final int port) {
        this.port = port;
    }

    /**
     * 设置 VIP 地址。
     *
     * @param vipAddress VIP 地址（可为 null）
     */
    public void setVipAddress(final String vipAddress) {
        this.vipAddress = vipAddress;
    }

    /**
     * 设置通信协议。
     *
     * @param protocol 协议字符串
     */
    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    /**
     * 设置节点状态。
     *
     * @param nodeStatus 节点状态枚举
     */
    public void setNodeStatus(final TlqNodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    /**
     * 设置节点描述。
     *
     * @param description 描述信息（可为 null）
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * 设置最近心跳时间。
     *
     * @param lastHeartbeat 心跳时间（可为 null）
     */
    public void setLastHeartbeat(final LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
