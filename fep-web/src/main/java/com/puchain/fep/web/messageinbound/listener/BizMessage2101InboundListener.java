package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.batch.DataTransfer2101;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 2101 inbound consumer — FR-MSG-2101 模式6（PRD §4.3 line 770 + §1.4 line 863）。
 *
 * <p>HNDEMP→FEP 数据推送（2101）→ FEP 返回 9120 应答 + 自行处理（无业务回执）。
 * 行为见 {@link AbstractAck9120InboundListener}；{@code DataTransfer2101} 不携带业务
 * SerialNo（getSerialNo 返 null），dispatcher extractSerialNo fallback 到 transitionNo。
 * 幂等 key 命名空间前缀 {@code ACK-9120-2101-}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BizMessage2101InboundListener extends AbstractAck9120InboundListener {

    /**
     * Spring constructor injection.
     *
     * @param recordService   message record service, non-null
     * @param enqueuePort     outbound enqueue port, non-null
     * @param institutionCode 14-digit institution code from {@code fep.collector.institution-code}
     * @param clock           clock for entrustDate, non-null
     */
    public BizMessage2101InboundListener(
            final BizMessageRecordService recordService,
            final OutboundMessageEnqueuePort enqueuePort,
            @Value("${fep.collector.institution-code:}") final String institutionCode,
            final Clock clock) {
        super(recordService, enqueuePort, institutionCode, clock);
    }

    @Override
    protected MessageType messageType() {
        return MessageType.MSG_2101;
    }

    @Override
    protected String messageCode() {
        return "2101";
    }

    @Override
    protected Class<?> bodyClass() {
        return DataTransfer2101.class;
    }
}
