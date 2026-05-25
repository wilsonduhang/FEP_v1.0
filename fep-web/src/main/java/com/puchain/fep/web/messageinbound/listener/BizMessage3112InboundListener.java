package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * 3112 inbound consumer — FR-MSG-3112 银行被动接收（PRD §4.6:841 + §4.7:862 模式5）。
 *
 * <p>Phase 1：持久化 + 9120 ack（行为见 {@link AbstractAck9120InboundListener}）；3113 回执
 * 内容组装 deferred roadmap Plan C（需行内授信查询接口规范）。幂等前缀 {@code ACK-9120-3112-}。
 * {@code HxqyCreditAmt3112} 携带真实业务 SerialNo（dispatcher 返回业务 serialNo 非 fallback）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BizMessage3112InboundListener extends AbstractAck9120InboundListener {

    /**
     * Spring constructor injection.
     *
     * @param recordService   message record service, non-null
     * @param enqueuePort     outbound enqueue port, non-null
     * @param institutionCode 14-digit institution code from {@code fep.collector.institution-code}
     * @param clock           clock for entrustDate, non-null
     */
    public BizMessage3112InboundListener(
            final BizMessageRecordService recordService,
            final OutboundMessageEnqueuePort enqueuePort,
            @Value("${fep.collector.institution-code:}") final String institutionCode,
            final Clock clock) {
        super(recordService, enqueuePort, institutionCode, clock);
    }

    @Override
    protected MessageType messageType() {
        return MessageType.MSG_3112;
    }

    @Override
    protected String messageCode() {
        return "3112";
    }

    @Override
    protected Class<?> bodyClass() {
        return HxqyCreditAmt3112.class;
    }
}
