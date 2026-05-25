package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import com.puchain.fep.web.outbound.consumer.OutboundTlqSender.OutboundSendOutcome;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * B1 Task 2 — {@link OutboundQueueRunnerImpl} 编排单测。
 *
 * <p>本测试仅断言 RunnerImpl 的 orchestration 行为：
 * read → build → sign → send → 成功委派 {@link OutboundStatusWriterService#recordSent} /
 * 失败委派 {@link OutboundStatusWriterService#recordFailure}。Tx 边界由 StatusWriter 自身的
 * {@code OutboundStatusWriterServiceTest} 与 {@link P5OutboundEndToEndIntegrationTest} 中
 * "TLQ send 调用时无活跃 Tx" 用例承担。</p>
 *
 * <p><b>Plan 偏离修正</b>：Plan §Task 2 Step 1 line 481/496 写
 * {@code OutboundSendOutcome.success(...)} / {@code .failure(...)} 工厂方法 — 实测
 * {@link OutboundSendOutcome} 是无工厂方法的 record，故改用 record 构造器
 * {@code new OutboundSendOutcome(boolean, String, String)}（msgId 在 failure 路径为 null）。</p>
 */
@ExtendWith(MockitoExtension.class)
class OutboundQueueRunnerImplTest {

    @Mock private OutboundQueueRepository repository;
    @Mock private OutboundCfxEnvelopeBuilder envelopeBuilder;
    @Mock private OutboundSignAdapter signAdapter;
    @Mock private OutboundTlqSender tlqSender;
    @Mock private OutboundStatusWriterService statusWriter;
    @Mock private OutboundMetrics metrics;

    @InjectMocks private OutboundQueueRunnerImpl runner;

    @Test
    void run_whenSendSucceeds_shouldDelegateToStatusWriterRecordSent() {
        final OutboundMessageQueueEntity entity = sampleEntity("Q1");
        when(repository.findById("Q1")).thenReturn(Optional.of(entity));
        when(envelopeBuilder.build(eq(entity), any(OutboundHeadFields.class)))
                .thenReturn(new OutboundCfxEnvelopeBuilder.EnvelopeBuildResult("<env/>", "20260525060000000001"));
        when(signAdapter.embedSignatureAsComment("<env/>")).thenReturn("<env signed/>");
        when(tlqSender.send("<env signed/>", "20260525060000000001"))
                .thenReturn(new OutboundSendOutcome(true, "20260525060000000001", "TLQ-OK"));

        runner.run("Q1");

        verify(statusWriter).recordSent(eq("Q1"), eq("20260525060000000001"), eq("TLQ-OK"), any());
        verify(statusWriter, never()).recordFailure(any(), any());
        verify(metrics).recordSent(any(Long.class));
    }

    @Test
    void run_whenSendReturnsFailure_shouldDelegateToStatusWriterRecordFailure() {
        final OutboundMessageQueueEntity entity = sampleEntity("Q2");
        when(repository.findById("Q2")).thenReturn(Optional.of(entity));
        when(envelopeBuilder.build(any(), any()))
                .thenReturn(new OutboundCfxEnvelopeBuilder.EnvelopeBuildResult("<env/>", "20260525060000000002"));
        when(signAdapter.embedSignatureAsComment(any())).thenReturn("<env signed/>");
        when(tlqSender.send(any(), any()))
                .thenReturn(new OutboundSendOutcome(false, null, "TLQ-FAIL"));

        runner.run("Q2");

        verify(statusWriter).recordFailure(eq("Q2"), any(Throwable.class));
        verify(statusWriter, never()).recordSent(any(), any(), any(), any());
    }

    @Test
    void run_whenBuilderThrows_shouldDelegateToStatusWriterRecordFailure() {
        final OutboundMessageQueueEntity entity = sampleEntity("Q3");
        when(repository.findById("Q3")).thenReturn(Optional.of(entity));
        when(envelopeBuilder.build(any(), any())).thenThrow(new RuntimeException("build boom"));

        runner.run("Q3");

        verify(statusWriter).recordFailure(eq("Q3"), any(RuntimeException.class));
    }

    @Test
    void run_whenQueueIdNotFound_shouldThrowIllegalStateException() {
        when(repository.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runner.run("MISSING"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MISSING");
        verify(statusWriter, never()).recordSent(any(), any(), any(), any());
        verify(statusWriter, never()).recordFailure(any(), any());
    }

    private OutboundMessageQueueEntity sampleEntity(final String id) {
        final OutboundMessageQueueEntity e = new OutboundMessageQueueEntity();
        e.setQueueId(id);
        // 必须是合法 OutboundHeadFields XML（含三必填字段 sendOrgCode/entrustDate/transitionNo）。
        // OutboundHeadXmlParser 用 record compact constructor 拒绝 null，self-closing 会触发
        // OUTBOUND_5106_HEAD_FIELDS_INVALID。Plan §Step 1 line 528 写 "<head/>" 是占位偏离 —
        // 实测 parser 严格按 @XmlRootElement(name="OutboundHeadFields")+ 三必填子字段反序列化。
        e.setMessageHeadXml("<OutboundHeadFields>"
                + "<sendOrgCode>BANK001</sendOrgCode>"
                + "<entrustDate>20260505</entrustDate>"
                + "<transitionNo>00000001</transitionNo>"
                + "</OutboundHeadFields>");
        e.setStatus("READY");
        return e;
    }
}
