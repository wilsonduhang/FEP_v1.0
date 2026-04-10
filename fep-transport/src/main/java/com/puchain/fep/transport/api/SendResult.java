package com.puchain.fep.transport.api;

/**
 * TLQ 消息发送结果。
 *
 * <p>不可变记录，封装发送操作的成功/失败状态、消息 ID 和错误信息。
 * 使用静态工厂方法 {@link #ok(String)} 和 {@link #fail(String, String)} 构造。</p>
 *
 * @param success 发送是否成功
 * @param msgId   消息 ID
 * @param error   失败时的错误信息，成功时为 {@code null}
 * @author FEP Team
 * @since 1.0.0
 */
public record SendResult(boolean success, String msgId, String error) {

    /**
     * 构造成功结果。
     *
     * @param msgId 消息 ID
     * @return 成功的发送结果
     */
    public static SendResult ok(final String msgId) {
        return new SendResult(true, msgId, null);
    }

    /**
     * 构造失败结果。
     *
     * @param msgId 消息 ID
     * @param error 错误信息
     * @return 失败的发送结果
     */
    public static SendResult fail(final String msgId, final String error) {
        return new SendResult(false, msgId, error);
    }
}
