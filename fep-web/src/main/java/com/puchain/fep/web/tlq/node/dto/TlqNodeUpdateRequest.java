package com.puchain.fep.web.tlq.node.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * TLQ 节点更新请求 DTO。
 *
 * <p>所有字段均为可选（partial update）；节点角色不可修改。
 * 参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TlqNodeUpdateRequest {

    /** 节点名称（可选）。 */
    private String nodeName;

    /** 主机 IP（可选）。 */
    private String hostIp;

    /** 监听端口（可选，1-65535）。 */
    @Min(value = 1, message = "端口必须在 1-65535 之间")
    @Max(value = 65535, message = "端口必须在 1-65535 之间")
    private Integer port;

    /** VIP 地址（可选）。 */
    private String vipAddress;

    /** 通信协议（可选）。 */
    private String protocol;

    /** 节点描述（可选）。 */
    private String description;

    /** 无参构造方法。 */
    public TlqNodeUpdateRequest() { /* for deserialization */ }

    /**
     * 获取节点名称。
     *
     * @return 节点名称（可为 null）
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
     * 获取主机 IP。
     *
     * @return 主机 IP（可为 null）
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
     * @return 端口号（可为 null）
     */
    public Integer getPort() {
        return port;
    }

    /**
     * 设置监听端口。
     *
     * @param port 端口号
     */
    public void setPort(final Integer port) {
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
     * @return 协议（可为 null）
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
