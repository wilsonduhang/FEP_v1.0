package com.puchain.fep.web.callback.http;

/**
 * 回调 HTTP 推送结果。
 *
 * <p>封装行内系统返回状态，供 {@link CallbackQueueRunner} 判断成败并更新队列条目。</p>
 *
 * @param success    行内返回 2xx 时为 {@code true}，非 2xx 或 IO 异常时为 {@code false}
 * @param statusCode HTTP 响应状态码；IO 异常（连接拒绝/超时等）时为 {@code 0}
 * @param error      失败时的简要描述（成功时为 {@code null}）
 * @author FEP Team
 * @since 1.0.0
 */
public record CallbackResult(boolean success, int statusCode, String error) {
}
