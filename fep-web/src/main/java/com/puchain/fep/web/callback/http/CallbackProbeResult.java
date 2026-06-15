package com.puchain.fep.web.callback.http;

/**
 * 凭证感知连通性探测结果（不含任何凭证明文/密文）。
 *
 * @param reachable   目标接口是否返回 2xx（HEAD 探测）
 * @param statusCode  HTTP 状态码（0 = 未建立 HTTP 响应，如 IO 失败 / 鉴权解析失败）
 * @param authApplied 是否注入了鉴权头（TOKEN/OAUTH2=true，NONE=false）
 * @param latencyMs   探测耗时（毫秒，含鉴权头解析 + 网络往返）
 * @param message     结果摘要（{@code ok} / {@code http <code>} / {@code io:<type>} / {@code auth:<type>} / {@code interrupted}）
 */
public record CallbackProbeResult(boolean reachable, int statusCode,
                                  boolean authApplied, long latencyMs, String message) {
}
