package com.puchain.fep.web.requeststate;

/**
 * Correlation key 归一工具：把 outbound / inbound 两侧的 transitionNo 规整为可逐字节比对的 canonical
 * 形式。S2 request-state tracking 子系统。
 *
 * <h3>设计前提：两侧 transitionNo 同源且均为 8 位业务流水号（Step 1 grep 实测结论）</h3>
 * <ul>
 *   <li><strong>Outbound 值来源</strong>：{@code JpaOutboundMessageEnqueueService}（line 145）写入
 *       {@code envelope.headFields().transitionNo()}，其上游业务头
 *       {@code RequestBusinessHead.setTransitionNo}（line 98-102）与 {@code AbstractRealHead}
 *       （line 105-112）在 setter 即用 {@code TRANSITION_NO_PATTERN} 强制「非 null 时必须为
 *       <strong>8 位数字</strong>」，否则抛 {@code IllegalArgumentException}。即写到队列的
 *       transitionNo 在结构上保证 8 位数字（或 null）。</li>
 *   <li><strong>Inbound 值来源</strong>：{@code InboundTransitionNoExtractor.extract}
 *       从同一 CFX 业务头 {@code /CFX/MSG/*}{@code /TransitionNo/text()} 读出真值并 {@code trim()}，
 *       即读出的也是同一 8 位业务流水号字段。</li>
 * </ul>
 *
 * <p>结论：两侧值同源（同一 8 位业务流水号字段），<strong>无 30→8 截断需求</strong>。归一只需做
 * 纯防御性 trim（去除 pretty-print/mixed-content 可能引入的边界空白），即可让两侧字符串字节相等。
 * 本类<strong>不</strong>做长度/格式校验、<strong>不</strong>截断、<strong>不</strong>补位：若某侧出现
 * 非 8 位脏值（理论上 setter 已拦截，但防御性留口），归一只返回 trim 后的原值，由调用方
 * （{@link RequestStateService#markResultReceived}）按「查不到对应 correlation key」自然落入
 * unmatched 分支，而非由本类强转造成错误匹配。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class TransitionNoNormalizer {

    private TransitionNoNormalizer() {
        // utility class — no instances
    }

    /**
     * 归一 transitionNo：纯防御 trim（非截断、非补位、非格式校验）。
     *
     * @param raw 原始 transitionNo，可空
     * @return trim 后的非空值；{@code null} 或 trim 后为空串均返回 {@code null}
     *         （供调用方按 unmatched 处理，不强转 / 不截断）
     */
    public static String canonical(final String raw) {
        if (raw == null) {
            return null;
        }
        final String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
