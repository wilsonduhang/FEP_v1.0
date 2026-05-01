package com.puchain.fep.processor.intake.port;

import java.util.Objects;

/**
 * 出站报文跨模块入队载体（不可变 record，intake.port 跨模块契约）。
 *
 * <p>P4 fep-collector 在 {@code PayloadAssembler.assemble} 后构造此 envelope，
 * 经 {@link OutboundMessageEnqueuePort#submit(OutboundMessageEnvelope)} 交付
 * 给 fep-web 持久化层（T7a）；P5+ 队列消费侧再据此装配 CFX 报文上行。
 *
 * <p><b>字段语义：</b>
 * <ul>
 *   <li>{@code messageType}     — 报文类型，固定 4 位（如 "3101"）</li>
 *   <li>{@code direction}       — 流向，目前只有 {@link Direction#OUTBOUND}</li>
 *   <li>{@code idempotencyKey}  — 幂等键，固定 32 位 hex（来自
 *       {@code fep.collector.support.IdempotencyKeyGenerator}）</li>
 *   <li>{@code headFields}      — 报文头 3 字段（{@link OutboundHeadFields}）</li>
 *   <li>{@code messageBody}     — 报文体载荷，类型由 collector 决定（P5+ 按 messageType
 *       路由 JAXB context 完成 marshal），故声明为 {@link Object}</li>
 *   <li>{@code payloadDataType} — 业务侧载荷类型（如 {@code "INVOICE_CONTRACT_3101"}），
 *       与 {@code fep.collector.adapters[*].payloadDataType} 一致，便于审计追溯</li>
 *   <li>{@code sourceRef}       — 业务记录在源系统的引用（如行内主键 / 文件 offset），
 *       便于审计回溯与对账</li>
 * </ul>
 *
 * <p><b>messageBody 用 Object 而非泛型 / CfxBody 的取舍：</b>fep-collector 不允许
 * 依赖 fep-converter（架构 R2），无法 import CfxBody；用 Object 透传由 P5+ 消费侧
 * 按 messageType 强转为对应 Body 类型。
 *
 * <p>compact 构造函数对所有引用字段执行 {@link Objects#requireNonNull}。
 * {@code idempotencyKey} 不在此校验长度，由生成器侧保证（与 CollectionRecord 同款约定）。
 *
 * @param messageType     4 位报文类型（非 null）
 * @param direction       流向枚举（非 null）
 * @param idempotencyKey  32 位 hex 幂等键（非 null）
 * @param headFields      报文头三字段载体（非 null）
 * @param messageBody     报文体载荷对象（非 null；类型由 collector 决定）
 * @param payloadDataType 业务侧载荷类型（非 null）
 * @param sourceRef       源系统业务引用（非 null；空字符串允许）
 * @author FEP Team
 * @since 1.0.0
 */
public record OutboundMessageEnvelope(
        String messageType,
        Direction direction,
        String idempotencyKey,
        OutboundHeadFields headFields,
        Object messageBody,
        String payloadDataType,
        String sourceRef
) {

    /**
     * compact 构造函数 — null 校验所有引用字段。
     */
    public OutboundMessageEnvelope {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(headFields, "headFields");
        Objects.requireNonNull(messageBody, "messageBody");
        Objects.requireNonNull(payloadDataType, "payloadDataType");
        Objects.requireNonNull(sourceRef, "sourceRef");
    }
}
