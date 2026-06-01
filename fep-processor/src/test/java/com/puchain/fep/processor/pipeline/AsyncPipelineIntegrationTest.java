package com.puchain.fep.processor.pipeline;

import com.puchain.fep.common.util.FepConstants;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.InMemoryMessageProcessStore;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.XsdValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for the async message processing pipeline.
 * Validates the full request-then-response flow for all three supply-chain
 * query pairs (3001/3002, 3003/3004, 3005/3006), XSD failure path, and
 * a performance baseline of 100 inbound operations.
 *
 * <p>No Spring context required; components are wired manually.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS)
/**
 * R-2 (2026-05-07): 文本块内嵌入字面量 "A1000143000104" 是 HNDEMP 中心节点代码 fixture，与
 * {@link com.puchain.fep.common.util.FepConstants#HNDEMP_NODE_CODE} 同源。Java 文本块语法
 * (JEP 378) 不支持中段插入常量引用，故保留字面量于 fixture XML；新写测试请 import
 * {@code FepConstants} 并仅在 Java 表达式上下文中引用。
 */
class AsyncPipelineIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncPipelineIntegrationTest.class);

    private AsyncMessageProcessorService service;
    private InMemoryMessageProcessStore store;

    /** Shared HEAD template; callers replace {@code {{MSG_NO}}} with the actual message number. */
    private static final String HEAD_TEMPLATE = """
            <HEAD>
                <Version>1.0</Version>
                <SrcNode>12345678901234</SrcNode>
                <DesNode>A1000143000104</DesNode>
                <App>HNDEMP</App>
                <MsgNo>{{MSG_NO}}</MsgNo>
                <MsgId>20260417120000000001</MsgId>
                <CorrMsgId>20260417120000000001</CorrMsgId>
                <WorkDate>20260417</WorkDate>
            </HEAD>""";

    private static final String REQUEST_HEAD = """
                <SendOrgCode>12345678901234</SendOrgCode>
                <EntrustDate>20260417</EntrustDate>
                <TransitionNo>00000001</TransitionNo>""";

    private static final String RESPONSE_HEAD = """
                <SendOrgCode>12345678901234</SendOrgCode>
                <EntrustDate>20260417</EntrustDate>
                <TransitionNo>00000001</TransitionNo>
                <Result>00000</Result>""";

    /** 30-char SerialNo (Text, length=30). */
    private static final String SERIAL_NO = "SN2026041700000000000000000001";

    @BeforeEach
    void setUp() {
        XsdValidator validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
        store = new InMemoryMessageProcessStore();
        MessageStateMachine machine = new MessageStateMachine(store);
        service = new AsyncMessageProcessorService(validator, machine, store);
    }

    // ── 3001 → 3002 async flow ─────────────────────────────────────────

    @Test
    void asyncFlow_3001_3002_shouldCompleteSuccessfully() {
        byte[] requestXml = toBytes(cfx("3001", """
                <RealHead3001>
            """ + REQUEST_HEAD + """
                </RealHead3001>
                <ProgressQuery3001>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A</hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <QueryType>1</QueryType>
                    <QueryKey>KEY001</QueryKey>
                </ProgressQuery3001>"""));

        MessageProcessRecord inbound = service.processAsyncInbound(
                MessageType.MSG_3001, "TN001", requestXml);

        assertThat(inbound.getStatus()).isEqualTo(MessageProcessStatus.PROCESSING);
        assertThat(inbound.getErrorCode()).isNull();

        byte[] responseXml = toBytes(cfx("3002", """
                <RealHead3002>
            """ + RESPONSE_HEAD + """
                </RealHead3002>
                <ProgressQueryReturn3002>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A</hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <QueryType>1</QueryType>
                    <QueryKey>KEY001</QueryKey>
                    <ReturnCode>01</ReturnCode>
                </ProgressQueryReturn3002>"""));

        MessageProcessRecord completed = service.completeWithResponse(
                "TN001", MessageType.MSG_3002, responseXml);

        assertThat(completed.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
        assertThat(completed.getErrorCode()).isNull();
    }

    // ── 3003 → 3004 async flow ─────────────────────────────────────────

    @Test
    void asyncFlow_3003_3004_shouldCompleteSuccessfully() {
        byte[] requestXml = toBytes(cfx("3003", """
                <RealHead3003>
            """ + REQUEST_HEAD + """
                </RealHead3003>
                <pzInfoQuery3003>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A</hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <pzNo>PZ202604170001</pzNo>
                </pzInfoQuery3003>"""));

        MessageProcessRecord inbound = service.processAsyncInbound(
                MessageType.MSG_3003, "TN002", requestXml);

        assertThat(inbound.getStatus()).isEqualTo(MessageProcessStatus.PROCESSING);
        assertThat(inbound.getErrorCode()).isNull();

        byte[] responseXml = toBytes(cfx("3004", """
                <RealHead3004>
            """ + RESPONSE_HEAD + """
                </RealHead3004>
                <pzInfoReturn3004>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A</hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <pzNo>PZ202604170001</pzNo>
                    <pzState>1</pzState>
                    <pzrzState>1</pzrzState>
                    <pzrzStatusInfo>
                        <pzNo>PZ202604170001</pzNo>
                        <rzPhaseCode>01</rzPhaseCode>
                        <BankNodeCode>12345678901234</BankNodeCode>
                    </pzrzStatusInfo>
                    <zpzAllInfo>
                        <SerialNumber>1</SerialNumber>
                        <pzNo>PZ202604170001SUB1</pzNo>
                        <pzClass>01</pzClass>
                        <qyAssignName>\u8F6C\u8BA9\u65B9\u4F01\u4E1A</qyAssignName>
                        <qyAssignCode>123456789012345678</qyAssignCode>
                        <qyRecvName>\u63A5\u6536\u65B9\u4F01\u4E1A</qyRecvName>
                        <qyRecvCode>987654321098765432</qyRecvCode>
                        <Amt>1000.00</Amt>
                        <UpdateDate>20260417</UpdateDate>
                        <pzFunction>001</pzFunction>
                        <pzState>1</pzState>
                        <pzrzState>1</pzrzState>
                        <pzMajorNo>PZ202604170001</pzMajorNo>
                        <LoanAmt>500.00</LoanAmt>
                        <SubState>1</SubState>
                    </zpzAllInfo>
                </pzInfoReturn3004>"""));

        MessageProcessRecord completed = service.completeWithResponse(
                "TN002", MessageType.MSG_3004, responseXml);

        assertThat(completed.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
        assertThat(completed.getErrorCode()).isNull();
    }

    // ── 3005 → 3006 async flow ─────────────────────────────────────────

    @Test
    void asyncFlow_3005_3006_shouldCompleteSuccessfully() {
        byte[] requestXml = toBytes(cfx("3005", """
                <RealHead3005>
            """ + REQUEST_HEAD + """
                </RealHead3005>
                <qyAccQuery3005>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <qyAccName>\u6D4B\u8BD5\u4F01\u4E1A\u8D26\u6237</qyAccName>
                    <qyAccCode>1234567890123456</qyAccCode>
                </qyAccQuery3005>"""));

        MessageProcessRecord inbound = service.processAsyncInbound(
                MessageType.MSG_3005, "TN003", requestXml);

        assertThat(inbound.getStatus()).isEqualTo(MessageProcessStatus.PROCESSING);
        assertThat(inbound.getErrorCode()).isNull();

        byte[] responseXml = toBytes(cfx("3006", """
                <RealHead3006>
            """ + RESPONSE_HEAD + """
                </RealHead3006>
                <qyAccQueryReturn3006>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <qyAccName>\u6D4B\u8BD5\u4F01\u4E1A\u8D26\u6237</qyAccName>
                    <qyAccCode>1234567890123456</qyAccCode>
                    <AccReturnCode>01</AccReturnCode>
                </qyAccQueryReturn3006>"""));

        MessageProcessRecord completed = service.completeWithResponse(
                "TN003", MessageType.MSG_3006, responseXml);

        assertThat(completed.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
        assertThat(completed.getErrorCode()).isNull();
    }

    // ── XSD failure path ───────────────────────────────────────────────

    @Test
    void asyncFlow_xsdFailure_shouldReturnFailed() {
        byte[] invalidXml = "<CFX><HEAD><Version>1.0</Version></HEAD></CFX>"
                .getBytes(StandardCharsets.UTF_8);

        MessageProcessRecord result = service.processAsyncInbound(
                MessageType.MSG_3001, "TN004", invalidXml);

        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(result.getErrorCode()).isEqualTo("PROC_8501");
    }

    // ── performance baseline ───────────────────────────────────────────

    /**
     * Performance gate: 95th-percentile (P95) async inbound latency must be
     * below 15ms over a 100-iteration sample (after a 5-iteration warmup that
     * primes the XSD schema cache).
     *
     * <p>Replaces the previous mean&lt;5ms gate (TD12) which was flaky on
     * shared CI hardware: a single GC pause in the 100-sample run could push
     * the arithmetic mean over 5ms even though the steady-state path stayed
     * fast. P95 absorbs single outliers while still catching real
     * degradations (e.g. uncached JAXBContext, lost XSD cache).</p>
     *
     * @implNote 2026-05-27: skipped on macOS via {@link DisabledOnOs}. Root
     * cause: macOS host scheduler / GC pause / system load (spotlight,
     * TimeMachine, concurrent mvn fork JVMs) drive per-call latency outliers
     * into the top 5% of the 100-sample distribution, pushing P95 well over
     * 15ms (observed 17.94 - 122ms across daily reports; 2026-05-27 RED runs
     * captured P95=77.26ms / mean=26.51ms / max=140.59ms in baseline).
     * GHA Ubuntu CI runs stable at P95 = 1.14 - 3.14ms (max=5.02ms) across 4
     * cross-validation runs — 13x safety margin. See investigate report at
     * {@code /Users/muzhou/FEP/docs/daily_reports/2026-05-26-async-pipeline-p95-investigate-report.md}
     * (local-only, non-git-tracked — muzhou private workspace).
     *
     * <p>macOS developers who need to force-run this test locally:</p>
     * <ol>
     *   <li>IDE: right-click method → Run (IDE overrides condition evaluation)</li>
     *   <li>Temporarily comment out {@code @DisabledOnOs} annotation</li>
     *   <li>Run inside a Linux Docker container</li>
     * </ol>
     */
    @Test
    @DisabledOnOs(value = OS.MAC, disabledReason =
            "Wall-clock perf gate is sensitive to macOS host scheduler/GC variance "
                    + "(see @implNote). GHA Ubuntu CI provides the steady baseline.")
    void performanceBaseline_100AsyncInbound_shouldHaveP95LessThan15ms() {
        byte[] validXml = toBytes(cfx("3001", """
                <RealHead3001>
            """ + REQUEST_HEAD + """
                </RealHead3001>
                <ProgressQuery3001>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A</hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <QueryType>1</QueryType>
                    <QueryKey>KEY001</QueryKey>
                </ProgressQuery3001>"""));

        // Warmup: 5 iterations to prime XSD schema cache
        for (int i = 0; i < 5; i++) {
            service.processAsyncInbound(MessageType.MSG_3001, "TN-WARM-" + i, validXml);
        }

        // Timed run: 100 iterations, capture per-iteration latency
        final int iterations = 100;
        long[] latenciesNs = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            MessageProcessRecord record = service.processAsyncInbound(
                    MessageType.MSG_3001, "TN-PERF-" + i, validXml);
            latenciesNs[i] = System.nanoTime() - t0;
            assertThat(record.getStatus()).isEqualTo(MessageProcessStatus.PROCESSING);
        }

        Arrays.sort(latenciesNs);
        // P95 = 95th-percentile element (1-based index 95 in a sorted 100-sample -> 0-based index 94)
        long p95Ns = latenciesNs[(int) Math.ceil(0.95 * iterations) - 1];
        double p95Ms = p95Ns / 1_000_000.0;
        double meanMs = Arrays.stream(latenciesNs).sum() / 1_000_000.0 / iterations;
        double maxMs = latenciesNs[iterations - 1] / 1_000_000.0;
        LOG.info("[PERF] AsyncPipeline 100 inbound: P95={}ms mean={}ms max={}ms (target P95<15ms)",
                String.format("%.2f", p95Ms), String.format("%.2f", meanMs), String.format("%.2f", maxMs));

        assertThat(p95Ms)
                .as("P95 async inbound latency should be < 15ms, was %.2fms (mean=%.2fms)", p95Ms, meanMs)
                .isLessThan(15.0);
    }

    // ── helpers ─────────────────────────────────────────────────────────

    /**
     * Wraps MSG content into a complete CFX envelope.
     *
     * @param msgNo      the 4-digit message number
     * @param msgContent XML content inside {@code <MSG>}
     * @return complete CFX XML string
     */
    private static String cfx(final String msgNo, final String msgContent) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<CFX>\n"
                + HEAD_TEMPLATE.replace("{{MSG_NO}}", msgNo) + "\n"
                + "    <MSG>\n" + msgContent + "\n    </MSG>\n</CFX>";
    }

    private static byte[] toBytes(final String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
