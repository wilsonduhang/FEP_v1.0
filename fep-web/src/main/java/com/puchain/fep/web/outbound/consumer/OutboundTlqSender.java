package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Outbound TLQ 发送适配器：把已加签 CFX envelope 推送到 TLQ BATCH_SEND 通道。
 *
 * <p>调用链：Runner → SignAdapter → <b>OutboundTlqSender</b> → fep-transport TlqProducer</p>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>调 {@link BodyMsgIdGenerator} 生成 20 字符 body msgId（持久化到 outbound_message_queue.msg_id）</li>
 *   <li>构造 {@link TlqMessageAttributes#forBatch(String)}（持久化、不过期）的 {@link TlqMessage}，通道 {@link TlqChannel#BATCH_SEND}</li>
 *   <li>调用 {@link TlqProducer#send(TlqMessage)} 并把 {@link SendResult} 折叠为 ≤64 字符的摘要 {@code tlqSendResult}</li>
 * </ul>
 *
 * <p>{@code srcNode/desNode} 不通过 {@link TlqMessage} 入参传递 — 由 fep-transport
 * 内部 {@code QueueNameResolver} 按 channel + 配置解析队列名（HNDEMP A1000143000104）。
 * 因此 {@link #send(String)} 入参仅 {@code signedXml} 一个；msgNo 已嵌入 envelope head。</p>
 *
 * <p>追溯: PRD v1.3 §3.1 + §3.1.3 / FR-MSG-OUTBOUND-SEND</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundTlqSender {

    /** tlqSendResult 摘要最大字符数（持久化到 outbound_message_queue.tlq_send_result）。 */
    private static final int RESULT_MAX_LEN = 64;

    private final TlqProducer producer;

    private final BodyMsgIdGenerator msgIdGenerator;

    /**
     * 构造发送适配器。
     *
     * @param producer       TLQ Producer（fep-transport），不能为 null
     * @param msgIdGenerator body msgId 生成器，不能为 null
     * @throws NullPointerException 当任一依赖为 null
     */
    public OutboundTlqSender(final TlqProducer producer, final BodyMsgIdGenerator msgIdGenerator) {
        this.producer = Objects.requireNonNull(producer, "producer must not be null");
        this.msgIdGenerator = Objects.requireNonNull(msgIdGenerator, "msgIdGenerator must not be null");
    }

    /**
     * 把已加签 CFX envelope 通过 TLQ BATCH_SEND 通道发送给中心节点。
     *
     * @param signedXml 已嵌入 SM2 签名注释的完整 CFX envelope（UTF-8）；不能为 null
     * @return outcome：是否成功 / 本地 body msgId（持久化用）/ ≤64 字符的 broker 摘要
     * @throws NullPointerException 当 {@code signedXml} 为 null
     */
    public OutboundSendOutcome send(final String signedXml) {
        Objects.requireNonNull(signedXml, "signedXml must not be null");

        String msgId = msgIdGenerator.generate();
        TlqMessageAttributes attrs = TlqMessageAttributes.forBatch(msgId);
        TlqMessage message = new TlqMessage(signedXml, attrs, TlqChannel.BATCH_SEND);

        SendResult result = producer.send(message);
        String tlqResult = result.success()
            ? "ok:" + result.msgId()
            : "fail:" + result.error();
        if (tlqResult.length() > RESULT_MAX_LEN) {
            tlqResult = tlqResult.substring(0, RESULT_MAX_LEN);
        }
        return new OutboundSendOutcome(result.success(), msgId, tlqResult);
    }

    /**
     * 发送结果记录。
     *
     * @param success        TLQ Producer 是否报成功
     * @param msgId          本地生成的 20 字符 body msgId（持久化用，与 broker 返回的 msgId 不同）
     * @param tlqSendResult  ≤64 字符的摘要：成功 {@code ok:<broker_msg_id>}，失败 {@code fail:<error>}
     */
    public record OutboundSendOutcome(boolean success, String msgId, String tlqSendResult) {
    }
}
