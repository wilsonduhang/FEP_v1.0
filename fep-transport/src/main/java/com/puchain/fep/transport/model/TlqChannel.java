package com.puchain.fep.transport.model;

/**
 * TLQ 通信通道枚举。
 *
 * <p>定义 HNDEMP 消息交换的四种通道，每种通道绑定固定端口号。
 * 实时通道使用端口 20001，非实时（批量）通道使用端口 20002。</p>
 *
 * <p>参见 PRD v1.3 §3.1.1 四通道定义。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TlqChannel {

    /** 实时发送通道，端口 20001。 */
    REALTIME_SEND(20001, "实时发送"),

    /** 实时接收通道，端口 20001。 */
    REALTIME_RECEIVE(20001, "实时接收"),

    /** 非实时（批量）发送通道，端口 20002。 */
    BATCH_SEND(20002, "非实时发送"),

    /** 非实时（批量）接收通道，端口 20002。 */
    BATCH_RECEIVE(20002, "非实时接收");

    private final int port;
    private final String description;

    TlqChannel(int port, String description) {
        this.port = port;
        this.description = description;
    }

    /**
     * 获取通道绑定的端口号。
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 获取通道的中文描述。
     *
     * @return 中文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 判断是否为实时通道。
     *
     * @return 实时通道返回 {@code true}
     */
    public boolean isRealtime() {
        return this == REALTIME_SEND || this == REALTIME_RECEIVE;
    }

    /**
     * 判断是否为发送通道。
     *
     * @return 发送通道返回 {@code true}
     */
    public boolean isSend() {
        return this == REALTIME_SEND || this == BATCH_SEND;
    }
}
