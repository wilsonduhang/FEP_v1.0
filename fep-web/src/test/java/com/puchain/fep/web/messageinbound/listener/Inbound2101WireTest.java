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
 * 2101 inbound listener wire IT — TLQ producer→broker→TlqInboundListener
 * →InboundMessageDispatcher→InboundMessageProcessedEvent→{@link BizMessage2101InboundListener}
 * 端到端链路验证（FR-MSG-2101 闭环 with mock TLQ provider）。
 *
 * <p>P4-MSG-D Plan T2 (v2.1) — verifies that:
 * <ol>
 *   <li>AC-1: 在 mock TLQ provider 下，发送 2101 CFX 报文经 BATCH_RECEIVE
 *       通道一路触发 BizMessage2101InboundListener。</li>
 *   <li>AC-2: event 字段（messageType=MSG_2101 / transitionNo / serialNo）
 *       正确穿过链路。</li>
 *   <li>AC-3: 9120 ack envelope 入 {@code outbound_message_queue}：
 *       {@code messageType="9120"} + {@code messageBodyXml} 含
 *       {@code <OriMsgNo>{serialNo}</OriMsgNo>}。</li>
 *   <li>AC-4: {@link BizMessageRecord} 持久化成功：
 *       {@code messageCode="2101"} + {@code direction=INBOUND}。</li>
 * </ol>
 *
 * <p><b>SerialNo 派生（关键 wire 行为）</b>：{@code DataTransfer2101} 不携带业务
 * SerialNo（其 {@code getSerialNo()} 返回 {@code null}），dispatcher 的
 * {@code extractSerialNo} fallback 到 {@code transitionNo}（CFX HEAD &lt;MsgId&gt;
 * 末 8 位）。因此 IT 用 {@code transitionNo} 既做 record 查找键，也做 9120 ack
 * idempotencyKey 派生输入（{@code SHA-256("ACK-9120-" + transitionNo)} 前 32 位 hex）。
 * 测试输入端的 TLQ {@code msgId} 与持久层 serialNo 不同 — 此为
 * {@link com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher#extractSerialNo}
 * 设计契约（Plan v2.1 §SerialNo 派生说明 + DataTransfer2101.getSerialNo 实测）。</p>
 *
 * <p><b>profile 选择</b>：默认 dev profile — dev 加载 MockSignService / MockKeyService，
 * outbound 9120 envelope 加签步骤才能在 IT 中通过 enqueuePort。{@code @ActiveProfiles("test")}
 * 会让 SignAdapter 找不到 SignService bean 启动失败（参考
 * {@code P5OutboundEndToEndIntegrationTest} 设计）。</p>
 *
 * <p><b>@MockBean XsdValidator</b>：CFX 报文 fixture 用最小 DataTransfer2101 body
 * 满足 XSD minLength/maxLength（红线 {@code feedback_fixture_data_must_satisfy_xsd_constraints}），
 * 但 BatchHead2101 envelope 校验不在本 IT 范围；mock validator 返回 ok 让
 * SyncMessageProcessorService.processInbound 流水线直达 COMPLETED。真 XSD 校验已在
 * fep-processor XsdValidatorTest + P4-MSG-D T1 单测覆盖。</p>
 *
 * <p>Real broker E2E（TongTech profile）走独立 ADR sub-Plan
 * {@code docs/plans/2026-05-11-2101-e2e-real-broker-blocked-adr.md}（🚫 BLOCKED）。</p>
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
@DisplayName("Inbound 2101 wire IT — TLQ→listener→dispatcher→event→consumer 端到端")
class Inbound2101WireTest {

    /** 14-digit msgId datetime prefix (yyyyMMddHHmmss) — fixed for IT determinism. */
    private static final String MSGID_DATETIME_PREFIX = "20260511120000";

    /** Monotonically increasing 6-digit seq for CFX MsgId 末 6 位 — last 8 chars = transitionNo. */
    private static final AtomicLong SEQ = new AtomicLong(100000L);

    /** SHA-256 truncation length matching {@code BizMessage2101InboundListener.IDEMPOTENCY_KEY_HEX_LEN}. */
    private static final int IDEMPOTENCY_KEY_HEX_LEN = 32;

    @Autowired
    private TlqProducer producer;

    @Autowired
    private BizMessageRecordRepository recordRepo;

    @Autowired
    private OutboundMessageQueueRepository outboundRepo;

    /**
     * fixture 使用最小 DataTransfer2101 body — XSD MainClass/SecondClass minLength=2
     * 已满足，但 BatchHead2101 envelope 不在 fixture 内；mock validator 让 processor
     * pipeline 通过，专注验证 wire 路径。
     */
    @MockBean
    private XsdValidator xsdValidator;

    @Test
    @DisplayName("AC-1..4: 发 2101 CFX → record 持久化 + 9120 ack 入队 (mock TLQ provider)")
    void inbound2101_shouldPersistAndEnqueue9120() throws Exception {
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());

        final String msgIdSeq = String.format("%06d", SEQ.getAndIncrement());
        final String cfxMsgId = MSGID_DATETIME_PREFIX + msgIdSeq;
        // transitionNo = last 8 chars of CFX HEAD <MsgId> per TlqInboundListener.deriveTransitionNo
        final String transitionNo = cfxMsgId.substring(cfxMsgId.length() - 8);
        // DataTransfer2101 has no SerialNo → dispatcher.extractSerialNo falls back to transitionNo
        final String serialNo = transitionNo;

        final String cfxXml = buildCfxEnvelope2101(cfxMsgId);
        final TlqMessageAttributes attrs = TlqMessageAttributes.forBatch(cfxMsgId);
        final TlqMessage msg = new TlqMessage(cfxXml, attrs, TlqChannel.BATCH_RECEIVE);

        producer.send(msg);

        final String ackIdempotencyKey = deriveAckIdempotencyKey(serialNo);

        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            assertThat(recordRepo.findBySerialNo(serialNo))
                    .as("BizMessageRecord must be persisted for serialNo=%s "
                            + "(AC-4 + wire listener chain triggered)", serialNo)
                    .isPresent();
            assertThat(outboundRepo.findByIdempotencyKey(ackIdempotencyKey))
                    .as("9120 ack envelope must be enqueued with "
                            + "idempotencyKey=SHA-256(ACK-9120-%s)[0:32]=%s (AC-3)",
                            serialNo, ackIdempotencyKey)
                    .isPresent();
        });

        final BizMessageRecord record = recordRepo.findBySerialNo(serialNo).orElseThrow();
        assertThat(record.getMessageCode())
                .as("AC-4: persisted messageCode")
                .isEqualTo("2101");
        assertThat(record.getDirection())
                .as("AC-4: persisted direction (INBOUND)")
                .isEqualTo(MessageDirection.INBOUND);

        final OutboundMessageQueueEntity outbound = outboundRepo
                .findByIdempotencyKey(ackIdempotencyKey).orElseThrow();
        assertThat(outbound.getMessageType())
                .as("AC-3: 9120 ack messageType")
                .isEqualTo("9120");
        assertThat(outbound.getMessageBodyXml())
                .as("AC-3: 9120 ack body XML must echo OriMsgNo=%s (serialNo of 2101 source)", serialNo)
                .contains("<OriMsgNo>" + serialNo + "</OriMsgNo>");
    }

    /**
     * Build a full CFX envelope for 2101 — must include {@code <CFX><HEAD>...
     * </HEAD><MSG><DataTransfer2101>...</DataTransfer2101></MSG></CFX>} so
     * {@code XmlCodec.unmarshal} succeeds and dispatcher routes via
     * {@code <MsgNo>2101</MsgNo>}.
     *
     * <p>DataTransfer2101 body fixture meets XSD constraints (DataTransfer2101.java
     * line 36-53): MainClass/SecondClass Token 2-16, Period/Type Number 1-2 digits,
     * FileDate 8-digit yyyyMMdd. SrcNode/DesNode are 14-digit per CommonHead
     * NODE_CODE_LENGTH constraint.</p>
     *
     * @param cfxMsgId 20-digit CFX HEAD MsgId (yyyyMMddHHmmss + 6-digit seq); last
     *                 8 chars become the dispatcher transitionNo
     * @return CFX envelope XML
     */
    private static String buildCfxEnvelope2101(final String cfxMsgId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX>"
                + "<HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
                + "<DesNode>12345678901234</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>2101</MsgNo>"
                + "<MsgId>" + cfxMsgId + "</MsgId>"
                + "<CorrMsgId></CorrMsgId>"
                + "<WorkDate>20260511</WorkDate>"
                + "</HEAD>"
                + "<MSG>"
                + "<DataTransfer2101>"
                + "<MainClass>FINANCE</MainClass>"
                + "<SecondClass>LOAN_REPORT</SecondClass>"
                + "<Period>1</Period>"
                + "<Type>1</Type>"
                + "<FileDate>20260511</FileDate>"
                + "</DataTransfer2101>"
                + "</MSG>"
                + "</CFX>";
    }

    /**
     * Deterministic 32-hex idempotency key — must match
     * {@code BizMessage2101InboundListener.deriveAckIdempotencyKey} verbatim
     * (prefix {@code ACK-9120-}, UTF-8 bytes, SHA-256, first 32 hex chars).
     *
     * @param serialNo the 2101 business serial number passed to the listener
     * @return 32-char hex string matching the outbound envelope's idempotencyKey
     * @throws Exception when SHA-256 algorithm is missing (will never happen on JVM)
     */
    private static String deriveAckIdempotencyKey(final String serialNo) throws Exception {
        final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        final byte[] hash = sha256.digest(
                ("ACK-9120-" + serialNo).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash).substring(0, IDEMPOTENCY_KEY_HEX_LEN);
    }
}
