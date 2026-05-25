package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.ArchiveReturnInfo3103;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 3103 inbound consumer — FR-MSG-3103 供应链受理建档回执（PRD §4.6:836 模式2 受理侧，终态）。
 *
 * <p>受理 = 持久化 + 9120 ack（行为见 {@link AbstractAck9120InboundListener}；muzhou Q1
 * 决策受理侧参照模式6 全返 9120）。无业务回执。幂等前缀 {@code ACK-9120-3103-}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BizMessage3103InboundListener extends AbstractAck9120InboundListener {

    /**
     * Spring constructor injection.
     *
     * @param recordService   message record service, non-null
     * @param enqueuePort     outbound enqueue port, non-null
     * @param institutionCode 14-digit institution code from {@code fep.collector.institution-code}
     * @param clock           clock for entrustDate, non-null
     */
    public BizMessage3103InboundListener(
            final BizMessageRecordService recordService,
            final OutboundMessageEnqueuePort enqueuePort,
            @Value("${fep.collector.institution-code:}") final String institutionCode,
            final Clock clock) {
        super(recordService, enqueuePort, institutionCode, clock);
    }

    @Override
    protected MessageType messageType() {
        return MessageType.MSG_3103;
    }

    @Override
    protected String messageCode() {
        return "3103";
    }

    @Override
    protected Class<?> bodyClass() {
        return ArchiveReturnInfo3103.class;
    }
}
