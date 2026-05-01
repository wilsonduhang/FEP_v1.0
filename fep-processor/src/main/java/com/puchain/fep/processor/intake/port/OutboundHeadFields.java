package com.puchain.fep.processor.intake.port;

import java.util.Objects;

/**
 * 出站报文头三字段载体（不可变 record，intake.port 跨模块契约）。
 *
 * <p>由 P4 fep-collector 数据采集层在组装阶段构造，传入
 * {@link OutboundMessageEnqueuePort#submit(OutboundMessageEnvelope)} 后由
 * fep-web 持久化为 {@code message_head_xml} 列。P5+ 队列消费时再映射为
 * fep-converter {@code RequestBusinessHead} 的对应字段。
 *
 * <p><b>字段语义：</b>
 * <ul>
 *   <li>{@code sendOrgCode} — 发送方机构代码（来自 {@code fep.collector.institution-code}）</li>
 *   <li>{@code entrustDate} — 委托日期，固定 8 位 {@code yyyyMMdd}</li>
 *   <li>{@code transitionNo} — 业务流水号，固定 8 位 numeric（PRD §3.2.3）</li>
 * </ul>
 *
 * <p><b>不直接依赖 fep-converter 的设计取舍：</b>fep-collector 不允许依赖
 * fep-converter（CollectorArchitectureTest R2 守护），因此本 record 字段虽与
 * fep-converter {@code RequestBusinessHead} 对齐但不复用类型 — P5+ 消费侧负责映射。
 *
 * <p>compact 构造函数对所有字段执行 {@link Objects#requireNonNull}。
 *
 * @param sendOrgCode  发送方机构代码（非 null / 非空）
 * @param entrustDate  委托日期 yyyyMMdd（非 null，由调用方保证 8 位格式）
 * @param transitionNo 业务流水号 8 位 numeric（非 null，由调用方保证格式）
 * @author FEP Team
 * @since 1.0.0
 */
public record OutboundHeadFields(
        String sendOrgCode,
        String entrustDate,
        String transitionNo
) {

    /**
     * compact 构造函数 — null 校验。
     */
    public OutboundHeadFields {
        Objects.requireNonNull(sendOrgCode, "sendOrgCode");
        Objects.requireNonNull(entrustDate, "entrustDate");
        Objects.requireNonNull(transitionNo, "transitionNo");
    }
}
