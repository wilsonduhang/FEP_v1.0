package com.puchain.fep.transport.model;

import java.util.Objects;

/**
 * TLQ 消息属性封装。
 *
 * <p>包含 TLQ 消息的系统属性（MsgId、CorrMsgId、Persistence、Expiry）
 * 和业务控制标志（zip 压缩、encrypt 加密）。</p>
 *
 * <p>注意：xmlstr / xmlstr1 / xmlstr2 不是属性，而是消息载荷，
 * 由 {@link TlqMessage#getPayload()} 承载。</p>
 *
 * <p>参见 PRD v1.3 §3.1.3 消息属性定义。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TlqMessageAttributes {

    /** 默认实时消息过期时间（秒）。 */
    private static final int DEFAULT_REALTIME_EXPIRY = 30;

    /** 批量消息不过期标识。 */
    private static final int NO_EXPIRY = -1;

    private String msgId;
    private String corrMsgId;
    private boolean persistence;
    private int expiry;
    private boolean zip;
    private boolean encrypt;

    /**
     * 默认构造：非持久化、30 秒过期、不压缩、不加密。
     */
    public TlqMessageAttributes() {
        this.persistence = false;
        this.expiry = DEFAULT_REALTIME_EXPIRY;
        this.zip = false;
        this.encrypt = false;
    }

    /**
     * 创建实时消息属性：非持久化、30 秒过期。
     *
     * @param msgId 消息 ID，不能为 {@code null}
     * @return 实时消息属性实例
     * @throws NullPointerException 如果 msgId 为 null
     */
    public static TlqMessageAttributes forRealtime(String msgId) {
        Objects.requireNonNull(msgId, "msgId must not be null");
        TlqMessageAttributes attrs = new TlqMessageAttributes();
        attrs.setMsgId(msgId);
        return attrs;
    }

    /**
     * 创建批量消息属性：持久化、不过期（expiry = -1）。
     *
     * @param msgId 消息 ID，不能为 {@code null}
     * @return 批量消息属性实例
     * @throws NullPointerException 如果 msgId 为 null
     */
    public static TlqMessageAttributes forBatch(String msgId) {
        Objects.requireNonNull(msgId, "msgId must not be null");
        TlqMessageAttributes attrs = new TlqMessageAttributes();
        attrs.setMsgId(msgId);
        attrs.setPersistence(true);
        attrs.setExpiry(NO_EXPIRY);
        return attrs;
    }

    /**
     * 获取消息 ID。
     *
     * @return 消息 ID
     */
    public String getMsgId() {
        return msgId;
    }

    /**
     * 设置消息 ID。
     *
     * @param msgId 消息 ID
     */
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    /**
     * 获取关联消息 ID（用于请求-响应关联）。
     *
     * @return 关联消息 ID
     */
    public String getCorrMsgId() {
        return corrMsgId;
    }

    /**
     * 设置关联消息 ID。
     *
     * @param corrMsgId 关联消息 ID
     */
    public void setCorrMsgId(String corrMsgId) {
        this.corrMsgId = corrMsgId;
    }

    /**
     * 是否持久化。
     *
     * @return 持久化返回 {@code true}
     */
    public boolean isPersistence() {
        return persistence;
    }

    /**
     * 设置是否持久化。
     *
     * @param persistence 是否持久化
     */
    public void setPersistence(boolean persistence) {
        this.persistence = persistence;
    }

    /**
     * 获取消息过期时间（秒），-1 表示不过期。
     *
     * @return 过期时间（秒）
     */
    public int getExpiry() {
        return expiry;
    }

    /**
     * 设置消息过期时间（秒）。
     *
     * @param expiry 过期时间，-1 表示不过期
     */
    public void setExpiry(int expiry) {
        this.expiry = expiry;
    }

    /**
     * 是否启用 ZIP 压缩。
     *
     * @return 启用压缩返回 {@code true}
     */
    public boolean isZip() {
        return zip;
    }

    /**
     * 设置是否启用 ZIP 压缩。
     *
     * @param zip 是否压缩
     */
    public void setZip(boolean zip) {
        this.zip = zip;
    }

    /**
     * 是否启用加密。
     *
     * @return 启用加密返回 {@code true}
     */
    public boolean isEncrypt() {
        return encrypt;
    }

    /**
     * 设置是否启用加密。
     *
     * @param encrypt 是否加密
     */
    public void setEncrypt(boolean encrypt) {
        this.encrypt = encrypt;
    }
}
