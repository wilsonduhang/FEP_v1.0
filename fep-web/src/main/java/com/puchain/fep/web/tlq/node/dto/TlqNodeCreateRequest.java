package com.puchain.fep.web.tlq.node.dto;

import com.puchain.fep.web.tlq.node.domain.TlqNodeRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * TLQ 节点创建请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TlqNodeCreateRequest {

    /** 节点名称（必填，唯一）。 */
    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    /** 节点角色（必填）。 */
    @NotNull(message = "节点角色不能为空")
    private TlqNodeRole nodeRole;

    /** 主机 IP（必填）。 */
    @NotBlank(message = "主机 IP 不能为空")
    private String hostIp;

    /** 监听端口（1-65535）。 */
    @Min(value = 1, message = "端口必须在 1-65535 之间")
    @Max(value = 65535, message = "端口必须在 1-65535 之间")
    private int port;

    /** VIP 地址（可选）。 */
    private String vipAddress;

    /** 通信协议（可选，默认 TCP）。 */
    private String protocol;

    /** 节点描述（可选）。 */
    private String description;

    /** 无参构造方法。 */
    public TlqNodeCreateRequest() { /* for deserialization */ }

    /**
     * 获取节点名称。
     *
     * @return 节点名称
     */
    public String getNodeName() {
        return nodeName;
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
     * 获取节点角色。
     *
     * @return 节点角色
     */
    public TlqNodeRole getNodeRole() {
        return nodeRole;
    }

    /**
     * 设置节点角色。
     *
     * @param nodeRole 节点角色
     */
    public void setNodeRole(final TlqNodeRole nodeRole) {
        this.nodeRole = nodeRole;
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
     * 设置主机 IP。
     *
     * @param hostIp 主机 IP
     */
    public void setHostIp(final String hostIp) {
        this.hostIp = hostIp;
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
     * 设置监听端口。
     *
     * @param port 端口号
     */
    public void setPort(final int port) {
        this.port = port;
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
     * 设置 VIP 地址。
     *
     * @param vipAddress VIP 地址
     */
    public void setVipAddress(final String vipAddress) {
        this.vipAddress = vipAddress;
    }

    /**
     * 获取通信协议。
     *
     * @return 协议（可为 null，默认 TCP）
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * 设置通信协议。
     *
     * @param protocol 协议
     */
    public void setProtocol(final String protocol) {
        this.protocol = protocol;
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
     * 设置节点描述。
     *
     * @param description 描述
     */
    public void setDescription(final String description) {
        this.description = description;
    }
}
