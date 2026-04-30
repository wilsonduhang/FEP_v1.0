package com.puchain.fep.collector.adapter.esb;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * {@link EsbCollectorAdapter} 不可变配置（Java record）。
 *
 * <p>覆盖 PRD v1.3 §2.2.2 数仓模式 + §2.1「ESB 适配器」配置（Plan §T5 #2）：
 * <ul>
 *   <li>{@code adapterId} — 与 {@code fep.collector.adapters[*].id} 一致（必填）</li>
 *   <li>{@code payloadDataType} — 报文数据类型（如 {@code INVOICE_CONTRACT_3101}），
 *       下游 {@code PayloadAssembler} 路由依据（必填）</li>
 *   <li>{@code endpoint} — ESB GET 接入点 URI（必填）</li>
 *   <li>{@code authHeaderName} — 鉴权 header 名（如 {@code Authorization}），不需鉴权时为 null</li>
 *   <li>{@code authHeaderValueRef} — 鉴权 header 值的<b>环境变量名</b>（不是值本身），
 *       由 {@link EsbCollectorAdapter} 启动时通过 {@link System#getenv(String)} 解析；
 *       不需鉴权时为 null</li>
 *   <li>{@code pollIntervalMillis} — 轮询间隔（仅记录配置语义；实际调度由 T6 CollectorScheduler 接管）</li>
 *   <li>{@code cursorParam} — 增量水位查询参数名（如 {@code lastSeen}），必填</li>
 *   <li>{@code initialCursor} — 首次运行水位字符串（必填）</li>
 *   <li>{@code timeout} — RestClient 单次调用超时；默认 10s（Plan §T5 #2）</li>
 * </ul>
 *
 * <p><b>命名说明：</b>Plan §T5 #2 文本写作 {@code timeoutMillis}，本类落地为
 * {@link Duration} 类型字段 {@code timeout} 以避免单位歧义并复用 JDK 类型系统
 * （compact constructor 校验 {@code timeout} 必须 &gt; 0）。
 *
 * <p>compact 构造函数对必填引用字段执行 {@link Objects#requireNonNull} 校验；
 * {@code timeout} 必须 &gt; 0。
 *
 * @author FEP Team
 * @since 1.0.0
 *
 * @param adapterId          适配器 ID（非 null）
 * @param payloadDataType    报文数据类型（非 null）
 * @param endpoint           ESB GET 接入点（非 null）
 * @param authHeaderName     鉴权 header 名（可为 null — 表示不需鉴权）
 * @param authHeaderValueRef 鉴权 header 值的环境变量名（可为 null — 表示不需鉴权）
 * @param pollIntervalMillis 轮询间隔（毫秒，仅记录；实际调度由 T6 完成）
 * @param cursorParam        增量水位查询参数名（非 null）
 * @param initialCursor      首次运行水位字符串（非 null）
 * @param timeout            RestClient 调用超时（非 null，&gt; 0）
 */
public record EsbAdapterConfig(
        String adapterId,
        String payloadDataType,
        URI endpoint,
        String authHeaderName,
        String authHeaderValueRef,
        long pollIntervalMillis,
        String cursorParam,
        String initialCursor,
        Duration timeout
) {

    /** 默认超时 — 10 秒（Plan §T5 #2）。 */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * compact 构造函数 — null 校验必填字段，并验证 {@code timeout} &gt; 0。
     */
    public EsbAdapterConfig {
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(payloadDataType, "payloadDataType");
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(cursorParam, "cursorParam");
        Objects.requireNonNull(initialCursor, "initialCursor");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must be > 0, got " + timeout);
        }
    }

    /**
     * 工厂方法 — {@code authHeaderName} / {@code authHeaderValueRef} 留空（不鉴权），
     * {@code pollIntervalMillis = 0}（由 T6 调度器覆盖），{@code timeout} 用 {@link #DEFAULT_TIMEOUT}。
     *
     * @param adapterId       适配器 ID（非 null）
     * @param payloadDataType 报文数据类型（非 null）
     * @param endpoint        ESB GET 接入点（非 null）
     * @param cursorParam     增量水位查询参数名（非 null）
     * @param initialCursor   首次运行水位字符串（非 null）
     * @return 新 {@link EsbAdapterConfig} 实例（不鉴权 / 默认超时）
     */
    public static EsbAdapterConfig withDefaults(
            final String adapterId,
            final String payloadDataType,
            final URI endpoint,
            final String cursorParam,
            final String initialCursor) {
        return new EsbAdapterConfig(
                adapterId, payloadDataType, endpoint,
                null, null,
                0L, cursorParam, initialCursor, DEFAULT_TIMEOUT);
    }
}
