package com.puchain.fep.transport.api;

/**
 * TLQ 连接工厂接口。
 *
 * <p>管理与 TLQ 服务端的连接生命周期。
 * 连接失败时抛出 {@code FepBusinessException(TRANS_7002)}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface TlqConnectionFactory {

    /**
     * 建立与 TLQ 服务端的连接。
     *
     * @throws com.puchain.fep.common.exception.FepBusinessException
     *         连接失败时抛出，错误码 TRANS_7002
     */
    void connect();

    /**
     * 断开与 TLQ 服务端的连接。
     */
    void disconnect();

    /**
     * 检查当前是否已连接。
     *
     * @return 已连接返回 {@code true}
     */
    boolean isConnected();
}
