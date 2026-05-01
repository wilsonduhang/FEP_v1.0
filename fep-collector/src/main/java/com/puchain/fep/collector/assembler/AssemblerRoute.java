package com.puchain.fep.collector.assembler;

import java.util.Objects;

/**
 * Assembler 路由记录（payloadDataType → messageType + FieldMapper 实现类）。
 *
 * <p>由 {@code Mode2Routes} / {@code Mode3Routes} 在启动期向 {@link RouteRegistry} 注册，
 * {@link DefaultPayloadAssembler#assemble} 据此查找：
 * <ol>
 *   <li>{@code messageType}（4 位）写入 {@link com.puchain.fep.processor.intake.port.OutboundMessageEnvelope}</li>
 *   <li>{@code fieldMapperClass} 用作 Spring bean 类型 lookup</li>
 * </ol>
 *
 * <p>compact 构造函数对所有字段执行 {@link Objects#requireNonNull}。
 *
 * @param messageType      4 位 HNDEMP 报文号（如 {@code "3101"}）
 * @param fieldMapperClass {@link FieldMapper} 子类，需注册为 Spring bean
 * @author FEP Team
 * @since 1.0.0
 */
public record AssemblerRoute(
        String messageType,
        Class<? extends FieldMapper> fieldMapperClass
) {

    /**
     * compact 构造函数 — null 校验。
     */
    public AssemblerRoute {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(fieldMapperClass, "fieldMapperClass");
    }
}
