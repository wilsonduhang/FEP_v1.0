package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3113;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 3113 inbound consumer — FR-MSG-3113 供应链受理授信查询回执（PRD §4.6:842 模式5 受理侧）。
 *
 * <p>受理 = 持久化 + 9120 ack（行为见 {@link AbstractAck9120InboundListener}；muzhou Q1
 * 决策受理侧参照模式6 全返 9120）。FEP 3112 query 的应答（终态），无业务回执。幂等前缀
 * {@code ACK-9120-3113-}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BizMessage3113InboundListener extends AbstractAck9120InboundListener {

    /**
     * Spring constructor injection.
     *
     * @param recordService   message record service, non-null
     * @param enqueuePort     outbound enqueue port, non-null
     * @param institutionCode 14-digit institution code from {@code fep.collector.institution-code}
     * @param clock           clock for entrustDate, non-null
     */
    public BizMessage3113InboundListener(
            final BizMessageRecordService recordService,
            final OutboundMessageEnqueuePort enqueuePort,
            @Value("${fep.collector.institution-code:}") final String institutionCode,
            final Clock clock) {
        super(recordService, enqueuePort, institutionCode, clock);
    }

    @Override
    protected MessageType messageType() {
        return MessageType.MSG_3113;
    }

    @Override
    protected String messageCode() {
        return "3113";
    }

    @Override
    protected Class<?> bodyClass() {
        return HxqyCreditAmt3113.class;
    }
}
