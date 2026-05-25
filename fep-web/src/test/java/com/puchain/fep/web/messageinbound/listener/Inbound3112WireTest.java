package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.domain.BizMessageRecord;
import com.puchain.fep.web.bizdata.record.repository.BizMessageRecordRepository;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import com.puchain.fep.web.outbound.OutboundMessageQueueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 3112 inbound listener wire IT — TLQ producer→broker→TlqInboundListener
 * →InboundMessageDispatcher→InboundMessageProcessedEvent→{@link BizMessage3112InboundListener}
 * 端到端链路验证（FR-MSG-3112 银行被动接收 模式5 闭环 with mock TLQ provider）。
 *
 * <p>P4-MSG-J Plan Task 4 — verifies:
 * <ol>
 *   <li>AC-1: mock TLQ provider 下发 3112 CFX 经 BATCH_RECEIVE 触发 listener。</li>
 *   <li>AC-2: {@link BizMessageRecord} 持久化 messageCode="3112" + direction=INBOUND。</li>
 *   <li>AC-3: 9120 ack 入 outbound_message_queue（messageType="9120" + body 含
 *       {@code <OriMsgNo>{serialNo}</OriMsgNo>}），idempotencyKey 前缀 ACK-9120-3112-。</li>
 * </ol>
 *
 * <p><b>SerialNo 派生</b>：与 2101 不同，{@code HxqyCreditAmt3112} 携带 XSD required 的
 * 业务 SerialNo（且 implements SerialNoBearing），dispatcher.extractSerialNo 返回 body
 * 内 {@code <SerialNo>} 值（非 transitionNo fallback）。故 record 查找键 + ack
 * idempotencyKey 派生输入均为 body serialNo。</p>
 *
 * <p><b>profile / @MockBean</b>：默认 dev profile（MockSignService/MockKeyService 让
 * 9120 outbound 加签通过）+ @MockBean XsdValidator 返回 ok（CFX fixture 专注 wire 路径，
 * 真 XSD 校验已在 fep-processor + Task 2 dispatch 单测覆盖）。与 {@link Inbound2101WireTest}
 * 同款配置。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.transport.provider=mock",
        "fep.collector.institution-code=12345678901234",
        "fep.collector.scheduling.enabled=false",
        "fep.outbound.queue.poll-interval-ms=99999",
        "fep.outbound.queue.poll-initial-delay-ms=99999",
        "management.health.redis.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Inbound 3112 wire IT — TLQ→listener→dispatcher→event→consumer 端到端")
class Inbound3112WireTest {

    /** 14-digit msgId datetime prefix (yyyyMMddHHmmss) — fixed for IT determinism. */
    private static final String MSGID_DATETIME_PREFIX = "20260524120000";

    /** Monotonically increasing 6-digit seq for CFX MsgId 末 6 位. */
    private static final AtomicLong SEQ = new AtomicLong(100000L);

    /** SHA-256 truncation length matching listener IDEMPOTENCY_KEY_HEX_LEN. */
    private static final int IDEMPOTENCY_KEY_HEX_LEN = 32;

    @Autowired
    private TlqProducer producer;

    @Autowired
    private BizMessageRecordRepository recordRepo;

    @Autowired
    private OutboundMessageQueueRepository outboundRepo;

    @MockBean
    private XsdValidator xsdValidator;

    @Test
    @DisplayName("AC-1..3: 发 3112 CFX → record 持久化 + 9120 ack 入队 (mock TLQ provider)")
    void inbound3112_shouldPersistAndEnqueue9120() {
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());

        final String msgIdSeq = String.format("%06d", SEQ.getAndIncrement());
        final String cfxMsgId = MSGID_DATETIME_PREFIX + msgIdSeq;
        // HxqyCreditAmt3112 carries a real SerialNo → dispatcher surfaces it (not transitionNo).
        final String serialNo = "SN3112" + msgIdSeq;

        final String cfxXml = buildCfxEnvelope3112(cfxMsgId, serialNo);
        final TlqMessageAttributes attrs = TlqMessageAttributes.forBatch(cfxMsgId);
        final TlqMessage msg = new TlqMessage(cfxXml, attrs, TlqChannel.BATCH_RECEIVE);

        producer.send(msg);

        final String ackIdempotencyKey = deriveAckIdempotencyKey(serialNo);

        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            assertThat(recordRepo.findBySerialNo(serialNo))
                    .as("BizMessageRecord must be persisted for serialNo=%s (AC-2, wire chain)", serialNo)
                    .isPresent();
            assertThat(outboundRepo.findByIdempotencyKey(ackIdempotencyKey))
                    .as("9120 ack envelope must be enqueued with idempotencyKey="
                            + "SHA-256(ACK-9120-3112-%s)[0:32]=%s (AC-3)", serialNo, ackIdempotencyKey)
                    .isPresent();
        });

        final BizMessageRecord record = recordRepo.findBySerialNo(serialNo).orElseThrow();
        assertThat(record.getMessageCode()).as("AC-2: messageCode").isEqualTo("3112");
        assertThat(record.getDirection()).as("AC-2: direction").isEqualTo(MessageDirection.INBOUND);

        final OutboundMessageQueueEntity outbound = outboundRepo
                .findByIdempotencyKey(ackIdempotencyKey).orElseThrow();
        assertThat(outbound.getMessageType()).as("AC-3: 9120 ack messageType").isEqualTo("9120");
        assertThat(outbound.getMessageBodyXml())
                .as("AC-3: 9120 ack body XML must echo OriMsgNo=%s", serialNo)
                .contains("<OriMsgNo>" + serialNo + "</OriMsgNo>");
    }

    /**
     * Build a full CFX envelope for 3112 — single body (no BatchHead), so
     * dispatcher's getBodies()::isInstance picks HxqyCreditAmt3112. Body fields
     * mirror HxqyInfo (hxqyName + hxqyCode). XsdValidator mocked, so structural
     * validity for JAXB unmarshal is sufficient.
     *
     * @param cfxMsgId 20-digit CFX HEAD MsgId; last 8 chars become transitionNo
     * @param serialNo business SerialNo placed in body (surfaced by dispatcher)
     * @return CFX envelope XML
     */
    private static String buildCfxEnvelope3112(final String cfxMsgId, final String serialNo) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX>"
                + "<HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
                + "<DesNode>12345678901234</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>3112</MsgNo>"
                + "<MsgId>" + cfxMsgId + "</MsgId>"
                + "<CorrMsgId></CorrMsgId>"
                + "<WorkDate>20260524</WorkDate>"
                + "</HEAD>"
                + "<MSG>"
                + "<hxqyCreditAmt3112>"
                + "<SerialNo>" + serialNo + "</SerialNo>"
                + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
                + "<DesNodeCode>12345678901234</DesNodeCode>"
                + "<QueryDate>20260524</QueryDate>"
                + "<hxqyInfoNum>1</hxqyInfoNum>"
                + "<hxqyInfo>"
                + "<hxqyName>核心企业测试</hxqyName>"
                + "<hxqyCode>91110000100000000X</hxqyCode>"
                + "</hxqyInfo>"
                + "</hxqyCreditAmt3112>"
                + "</MSG>"
                + "</CFX>";
    }

    /** Mirror of {@code BizMessage3112InboundListener.deriveAckIdempotencyKey} (prefix ACK-9120-3112-). */
    private static String deriveAckIdempotencyKey(final String serialNo) {
        try {
            final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            final byte[] hash = sha256.digest(
                    ("ACK-9120-3112-" + serialNo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, IDEMPOTENCY_KEY_HEX_LEN);
        } catch (final Exception e) {
            throw new IllegalStateException("SHA-256 missing", e);
        }
    }
}
