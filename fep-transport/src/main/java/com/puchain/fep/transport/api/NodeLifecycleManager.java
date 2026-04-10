package com.puchain.fep.transport.api;

import com.puchain.fep.transport.model.NodeState;

/**
 * TLQ 节点生命周期管理接口（PRD §3.7）。
 *
 * <p>负责节点登录（9006 报文）、登出（9008 报文）和心跳（9005 报文）处理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface NodeLifecycleManager {

    /**
     * 节点登录，发送 9006 报文。
     *
     * @return 登录成功返回 {@code true}
     */
    boolean login();

    /**
     * 节点登出，发送 9008 报文。
     *
     * @return 登出成功返回 {@code true}
     */
    boolean logout();

    /**
     * 处理心跳，响应 9005 报文。
     */
    void handleHeartbeat();

    /**
     * 获取当前节点状态。
     *
     * @return 节点状态
     */
    NodeState getState();
}
