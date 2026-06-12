package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.util.FepConstants;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * P4-MSG-K inbound 受理批处理 wire IT — 4 报文（3105/3009/3103/3113）参数化端到端
 * 链路验证: TLQ producer→broker→TlqInboundListener→InboundMessageDispatcher→
 * InboundMessageProcessedEvent→{@link AbstractAck9120InboundListener} 子类。
 *
 * <p>对照 {@link Inbound3112WireTest}（同包, known-green）的全链路结构 + 复用
 * {@link InboundListenerWireTest} 的 {@code @MethodSource} 参数化风格，覆盖 4 个
 * {@code AbstractAck9120InboundListener} 薄子类受理闭环（muzhou Q1 决策受理侧
 * 参照 2101 模式6 强制返 9120）。每参数验证:
 * <ol>
 *   <li>AC-1: mock TLQ provider 下发 {@code <code>} CFX 经 BATCH_RECEIVE 触发对应 listener。</li>
 *   <li>AC-2: {@link BizMessageRecord} 持久化 messageCode={@code <code>} + direction=INBOUND
 *       (按 body 真实 SerialNo 查找，4 报文均 implements SerialNoBearing → 非 transitionNo
 *       fallback)。</li>
 *   <li>AC-3: 9120 ack 入 outbound_message_queue（messageType="9120" + body 含
 *       {@code <OriMsgNo>{serialNo}</OriMsgNo>}），idempotencyKey ==
 *       {@link AckIdempotencyKeys#derive(String, String)}（前缀 {@code ACK-9120-<code>-}）。</li>
 * </ol>
 *
 * <p><b>disjoint seed 强制约定</b>（Task 1 quality review，防真 flake）：每个 wire IT
 * 的 transitionNo = CFX MsgId 末 8 位。{@code Inbound2101WireTest} 用 SEQ seed 100000L
 * (transitionNo 00100000)、{@code Inbound3112WireTest} 用 200000L (00200000)，本 IT 用
 * 300000L → 4 参数产出 00300000/00300001/00300002/00300003，三方互不碰撞。
 * {@code SyncMessageProcessorService} 以 transitionNo 去重，同 fork 内 transitionNo 碰撞
 * 会触发 "duplicate transitionNo" {@code IllegalStateException}（rerunFailingTestsCount=1
 * 仅能救一次），故跨 IT seed 必须 disjoint。各参数另用唯一 body serialNo（{@code SNK<code><seq>}）
 * 防跨参数 record/ack bleed。</p>
 *
 * <p><b>真 XsdValidator（R-NEW-1 起）/ profile</b>：自 2026-05-26 R-NEW-1 起，本测试
 * 不再 {@code @MockBean XsdValidator}，fixture 4 body + wrapCfx 满足各 XSD 完整约束
 * （HEAD CorrMsgId 20 零、BatchHead&lt;code&gt;/RealHead3009 RequestHead/ResponseHead 按
 * code 路由、body SerialNo length=30 pad）。默认 dev profile
 * （MockSignService/MockKeyService 让 9120 outbound 加签通过）。与 sibling
 * {@link Inbound2101WireTest}/{@link Inbound3112WireTest} 同 cache key（R-NEW-1 统一）。</p>
 *
 * <p>PRD 依据: v1.3 §4.6:833/836/837/842（受理闭环）+ §4.7 muzhou Q1。</p>
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
@DisplayName("Inbound 9120-ack batch wire IT — 3105/3009/3103/3113 TLQ→listener→event→consumer 端到端")
class InboundAck9120BatchWireTest {

    /** 14-digit msgId datetime prefix (yyyyMMddHHmmss) — fixed for IT determinism. */
    private static final String MSGID_DATETIME_PREFIX = "20260526120000";

    /**
     * Monotonically increasing 6-digit seq for CFX MsgId 末 6 位 — last 8 chars = transitionNo.
     * Seed 300000L → 00300000/00300001/00300002/00300003: disjoint from Inbound2101WireTest
     * (00100000) and Inbound3112WireTest (00200000) so no same-fork transitionNo collision.
     */
    private static final AtomicLong SEQ = new AtomicLong(300000L);

    @Autowired
    private TlqProducer producer;

    @Autowired
    private BizMessageRecordRepository recordRepo;

    @Autowired
    private OutboundMessageQueueRepository outboundRepo;

    /**
     * 4 报文测试矩阵: msgNo + CFX body 模板（{SERIAL} 占位）。HEAD 由 {@code wrapCfx}
     * 统一封装（含 {MSGID} 占位）。
     */
    static Stream<Arguments> inboundMatrix() {
        return Stream.of(
                Arguments.of("3105", BODY_3105_TEMPLATE),
                Arguments.of("3009", BODY_3009_TEMPLATE),
                Arguments.of("3103", BODY_3103_TEMPLATE),
                Arguments.of("3113", BODY_3113_TEMPLATE));
    }

    @ParameterizedTest(name = "[{index}] msgNo={0} 受理 → record INBOUND/{0} + 9120 ack(ACK-9120-{0}-) 入队")
    @MethodSource("inboundMatrix")
    @DisplayName("AC-1..3: 发 <code> CFX → record 持久化 + 9120 ack 入队 (mock TLQ provider)")
    void inboundReport_shouldPersistAndEnqueue9120(final String code, final String bodyTemplate) {
        final String msgIdSeq = String.format("%06d", SEQ.getAndIncrement());
        final String cfxMsgId = MSGID_DATETIME_PREFIX + msgIdSeq;
        // Each report carries a real business SerialNo → dispatcher surfaces it (not transitionNo).
        // SerialNo per DataType.xsd length=30 → pad raw "SNK<code><seq>" (13 chars) to 30.
        final String rawSerial = "SNK" + code + msgIdSeq;
        final String serialNo = XsdTestSupport.pad30(rawSerial);

        final String cfxXml = wrapCfx(code, cfxMsgId, bodyTemplate.replace("{SERIAL}", serialNo));
        final TlqMessageAttributes attrs = TlqMessageAttributes.forBatch(cfxMsgId);
        final TlqMessage msg = new TlqMessage(cfxXml, attrs, TlqChannel.BATCH_RECEIVE);

        producer.send(msg);

        final String ackIdempotencyKey = AckIdempotencyKeys.derive(code, serialNo);

        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            assertThat(recordRepo.findBySerialNo(serialNo))
                    .as("BizMessageRecord must be persisted for msgNo=%s serialNo=%s "
                            + "(AC-2, wire chain triggered)", code, serialNo)
                    .isPresent();
            assertThat(outboundRepo.findByIdempotencyKey(ackIdempotencyKey))
                    .as("9120 ack envelope must be enqueued with idempotencyKey="
                            + "AckIdempotencyKeys.derive(%s,%s)=%s (AC-3)", code, serialNo, ackIdempotencyKey)
                    .isPresent();
        });

        final BizMessageRecord record = recordRepo.findBySerialNo(serialNo).orElseThrow();
        assertThat(record.getMessageCode()).as("AC-2: messageCode for msgNo=%s", code).isEqualTo(code);
        assertThat(record.getDirection()).as("AC-2: direction for msgNo=%s", code)
                .isEqualTo(MessageDirection.INBOUND);

        final OutboundMessageQueueEntity outbound = outboundRepo
                .findByIdempotencyKey(ackIdempotencyKey).orElseThrow();
        assertThat(outbound.getMessageType()).as("AC-3: 9120 ack messageType for msgNo=%s", code)
                .isEqualTo("9120");
        assertThat(outbound.getMessageBodyXml())
                .as("AC-3: 9120 ack body XML must echo OriMsgNo=%s (serialNo of %s source)", serialNo, code)
                .contains("<OriMsgNo>" + serialNo + "</OriMsgNo>");
    }

    /**
     * Wrap a body fragment in a full CFX envelope with HEAD routing via {@code <MsgNo>}
     * and code-routed BatchHead/RealHead between HEAD and body.
     *
     * @param code     4-digit message code placed in CFX HEAD MsgNo
     * @param cfxMsgId 20-digit CFX HEAD MsgId; last 8 chars become dispatcher transitionNo
     * @param body     the MSG body fragment (single root element)
     * @return CFX envelope XML
     */
    private static String wrapCfx(final String code, final String cfxMsgId, final String body) {
        final String batchHead = buildBatchHeadByCode(code, cfxMsgId);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX>"
                + "<HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
                + "<DesNode>12345678901234</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>" + code + "</MsgNo>"
                + "<MsgId>" + cfxMsgId + "</MsgId>"
                + "<CorrMsgId>00000000000000000000</CorrMsgId>"
                + "<WorkDate>20260526</WorkDate>"
                + "</HEAD>"
                + "<MSG>"
                + batchHead
                + body
                + "</MSG>"
                + "</CFX>";
    }

    /**
     * Build the BatchHead/RealHead element per the message code:
     * <ul>
     *   <li>3105: BatchHead3105 (RequestHead)</li>
     *   <li>3009: RealHead3009 (RequestHead, special naming)</li>
     *   <li>3103/3113: BatchHead&lt;code&gt; (ResponseHead, includes Result)</li>
     * </ul>
     *
     * @param code      4-digit message code
     * @param cfxMsgId  20-digit CFX HEAD MsgId; last 8 chars = TransitionNo
     * @return BatchHead/RealHead XML fragment
     */
    private static String buildBatchHeadByCode(final String code, final String cfxMsgId) {
        final String transitionNo = cfxMsgId.substring(cfxMsgId.length() - 8);
        final String elementName = switch (code) {
            case "3009" -> "RealHead3009";
            default -> "BatchHead" + code;
        };
        final boolean responseHead = "3103".equals(code) || "3113".equals(code);
        final String resultElement = responseHead ? "<Result>90000</Result>" : "";
        return "<" + elementName + ">"
                + "<SendOrgCode>12345678901234</SendOrgCode>"
                + "<EntrustDate>20260526</EntrustDate>"
                + "<TransitionNo>" + transitionNo + "</TransitionNo>"
                + resultElement
                + "</" + elementName + ">";
    }

    /** 3105 融资申请 body — root {@code rzApplyInfo3105}; required scalars only (no nested blocks). */
    private static final String BODY_3105_TEMPLATE =
            "<rzApplyInfo3105>"
                    + "<SerialNo>{SERIAL}</SerialNo>"
                    + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
                    + "<DesNodeCode>12345678901234</DesNodeCode>"
                    + "<ApplyMode>1</ApplyMode>"
                    + "<PlatApplyNo>PLAT20260526001</PlatApplyNo>"
                    + "<StdBizMode>01</StdBizMode>"
                    + "<hxqyName>核心企业测试</hxqyName>"
                    + "<hxqyCode>91110000100000000X</hxqyCode>"
                    + "<rzpzNo>PZ20260526001</rzpzNo>"
                    + "<rzqyName>融资企业测试</rzqyName>"
                    + "<rzqyCode>91110000200000000Y</rzqyCode>"
                    + "<rzqyPlatNo>RZPLAT20260526001</rzqyPlatNo>"
                    + "</rzApplyInfo3105>";

    /** 3009 融资结果登记 body — root {@code rzReturnInfo3009}; required scalars only. */
    private static final String BODY_3009_TEMPLATE =
            "<rzReturnInfo3009>"
                    + "<SerialNo>{SERIAL}</SerialNo>"
                    + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
                    + "<DesNodeCode>12345678901234</DesNodeCode>"
                    + "<PlatApplyNo>PLAT20260526001</PlatApplyNo>"
                    + "<hxqyName>核心企业测试</hxqyName>"
                    + "<rzpzNo>PZ20260526001</rzpzNo>"
                    + "<rzPhaseCode>01</rzPhaseCode>"
                    + "</rzReturnInfo3009>";

    /** 3103 企业建档回执 body — root {@code ArchiveReturnInfo3103} (PascalCase!); required scalars only. */
    private static final String BODY_3103_TEMPLATE =
            "<ArchiveReturnInfo3103>"
                    + "<SerialNo>{SERIAL}</SerialNo>"
                    + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
                    + "<DesNodeCode>12345678901234</DesNodeCode>"
                    + "<CreationRetCode>01</CreationRetCode>"
                    + "<hxqyName>核心企业测试</hxqyName>"
                    + "<hxqyCode>91110000100000000X</hxqyCode>"
                    + "<rzqyName>融资企业测试</rzqyName>"
                    + "<rzqyCode>91110000200000000Y</rzqyCode>"
                    + "</ArchiveReturnInfo3103>";

    /** 3113 核心企业授信查询回执 body — root {@code hxqyCreditAmt3113}; required scalars + 1 CreditInfo. */
    private static final String BODY_3113_TEMPLATE =
            "<hxqyCreditAmt3113>"
                    + "<SerialNo>{SERIAL}</SerialNo>"
                    + "<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>"
                    + "<DesNodeCode>12345678901234</DesNodeCode>"
                    + "<QueryDate>20260526</QueryDate>"
                    + "<CreditInfoNum>1</CreditInfoNum>"
                    + "<CreditInfo>"
                    + "<hxqyName>核心企业测试</hxqyName>"
                    + "<hxqyCode>91110000100000000X</hxqyCode>"
                    + "<RetCode>00000</RetCode>"
                    + "</CreditInfo>"
                    + "</hxqyCreditAmt3113>";
}
