package com.puchain.fep.collector.assembler;

import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.intake.port.Direction;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 默认 {@link PayloadAssembler} 实现（Plan §T7b §1）。
 *
 * <p>编排：
 * <ol>
 *   <li>从 {@link RouteRegistry} 反查 {@link AssemblerRoute}</li>
 *   <li>从 {@link ApplicationContext} 取 {@link FieldMapper} bean → 调
 *       {@link FieldMapper#toMessageBody(java.util.Map)} 得 body POJO</li>
 *   <li>调 {@link HeadFieldsBuilder#build(CollectionRecord)} 派生 8 位 transitionNo /
 *       entrustDate / sendOrgCode</li>
 *   <li>组装 {@link OutboundMessageEnvelope} 返回</li>
 * </ol>
 *
 * <p><b>异常语义：</b>
 * <ul>
 *   <li>路由未注册 → {@link FepBusinessException}({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE})
 *       消息形如 {@code "no route for payloadDataType=<value>"}</li>
 *   <li>必填字段缺失（mapper 抛） → {@link FepBusinessException} 透传</li>
 *   <li>Stub mapper（如 3109/3116）→ {@link UnsupportedOperationException} 透传，
 *       上游 scheduler 转 FAILED 计数</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class DefaultPayloadAssembler implements PayloadAssembler {

    private final RouteRegistry routeRegistry;
    private final HeadFieldsBuilder headFieldsBuilder;
    private final ApplicationContext applicationContext;

    /**
     * 构造 assembler。
     *
     * @param routeRegistry      路由注册表（非 null）
     * @param headFieldsBuilder  头字段构造器（非 null）
     * @param applicationContext Spring 上下文（非 null；用于按 Class 反查 mapper bean）
     */
    public DefaultPayloadAssembler(final RouteRegistry routeRegistry,
                                   final HeadFieldsBuilder headFieldsBuilder,
                                   final ApplicationContext applicationContext) {
        this.routeRegistry = Objects.requireNonNull(routeRegistry, "routeRegistry");
        this.headFieldsBuilder = Objects.requireNonNull(headFieldsBuilder, "headFieldsBuilder");
        this.applicationContext = Objects.requireNonNull(applicationContext, "applicationContext");
    }

    @Override
    public OutboundMessageEnvelope assemble(final CollectionRecord record) {
        Objects.requireNonNull(record, "record");
        final String payloadDataType = record.getPayloadDataType();
        final AssemblerRoute route = routeRegistry.lookup(payloadDataType);
        if (route == null) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "no route for payloadDataType=" + LogSanitizer.sanitize(payloadDataType));
        }
        final FieldMapper mapper = applicationContext.getBean(route.fieldMapperClass());
        final Object body = mapper.toMessageBody(record.getRawData());
        Objects.requireNonNull(body,
                "FieldMapper " + route.fieldMapperClass().getName() + " returned null body");
        final OutboundHeadFields head = headFieldsBuilder.build(record);
        return new OutboundMessageEnvelope(
                route.messageType(),
                Direction.OUTBOUND,
                record.getIdempotencyKey(),
                head,
                body,
                payloadDataType,
                record.getSourceRef()
        );
    }
}
