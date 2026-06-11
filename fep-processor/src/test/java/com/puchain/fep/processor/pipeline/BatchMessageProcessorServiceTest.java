package com.puchain.fep.processor.pipeline;

import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.processor.state.IllegalMessageStateException;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.BusinessRuleValidator;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BatchMessageProcessorService unit tests (9 cases).
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>null msg → empty result</li>
 *   <li>空 batch → 零 counts</li>
 *   <li>单条成功 → completed</li>
 *   <li>多条全成功</li>
 *   <li>部分失败 → errors 记录 + 整体 FAILED</li>
 *   <li>全部失败</li>
 *   <li>大 payload → adapter split 被调用</li>
 *   <li>状态转移被拒 → IllegalMessageStateException 被吞并返回失败结果</li>
 *   <li>构造期 null 依赖 → NPE</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchMessageProcessorServiceTest {

    @Mock private XsdValidator xsdValidator;
    @Mock private BusinessRuleValidator businessRuleValidator;
    @Mock private MessageStateMachine stateMachine;
    @Mock private MessageProcessStore store;
    @Mock private BatchPayloadAdapter adapter;
    @Mock private OutboundWireShapeDispatcher wireShapeDispatcher;

    @InjectMocks private BatchMessageProcessorService service;

    @BeforeEach
    void setUp() {
        // mock 默认返回 null 会使 XSD 放行用例在第二关 NPE；统一 stub 放行（LENIENT 容忍未消费）
        when(businessRuleValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
    }

    @Test
    void process_nullMessage_shouldReturnEmptyResult() {
        assertThat(service.process(null)).isEqualTo(BatchResult.empty());
    }

    @Test
    void process_emptyBatch_shouldReturnZeroCounts() {
        CfxMessage msg = batchOf();
        BatchResult result = service.process(msg);
        assertThat(result.processedCount()).isZero();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void process_singleValidRecord_shouldMarkCompleted() {
        CfxMessage msg = batchOf(stub("r1"));
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        BatchResult result = service.process(msg);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.allSucceeded()).isTrue();
    }

    @Test
    void process_multipleValidRecords_shouldAllSucceed() {
        CfxMessage msg = batchOf(stub("r1"), stub("r2"), stub("r3"));
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        BatchResult result = service.process(msg);
        assertThat(result.processedCount()).isEqualTo(3);
        assertThat(result.failedCount()).isZero();
    }

    @Test
    void process_partialFailure_shouldRecordErrorsAndMarkFailed() {
        CfxMessage msg = batchOf(stub("ok1"), stub("bad"), stub("ok2"));
        when(xsdValidator.validate(any(), any()))
                .thenReturn(ValidationResult.ok())
                .thenReturn(ValidationResult.failed(List.of("XSD error in record 1")))
                .thenReturn(ValidationResult.ok());
        BatchResult result = service.process(msg);
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).index()).isEqualTo(1);
        assertThat(result.allSucceeded()).isFalse();
    }

    @Test
    void process_allFailed_shouldReportFullFailures() {
        CfxMessage msg = batchOf(stub("b1"), stub("b2"));
        when(xsdValidator.validate(any(), any()))
                .thenReturn(ValidationResult.failed(List.of("err")));
        BatchResult result = service.process(msg);
        assertThat(result.failedCount()).isEqualTo(2);
        assertThat(result.successCount()).isZero();
    }

    @Test
    void process_oversizedPayload_shouldInvokeAdapterSplit() {
        // 任意 body：marshal 后由 adapter 的 mock 判为 oversized
        CfxMessage msg = batchOf(stub("big"));
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(adapter.needsSplit(any(String.class))).thenReturn(true);
        service.process(msg);
        verify(adapter).needsSplit(any(String.class));
        verify(adapter).split(any(String.class));
    }

    @Test
    void process_stateTransitionRejected_shouldPropagateFailure() {
        CfxMessage msg = batchOf(stub("r1"));
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        // stateMachine 无 canTransition；非法转移通过 transition 内部 assertLegal 抛
        // IllegalMessageStateException（实测 MessageStateMachine.java:132）。
        // 用 anyString() 明确锁定 transition(String, Status) 重载，避免 Mockito 在
        // 两个 transition 重载间解析错误。
        doThrow(new IllegalMessageStateException("illegal transition"))
                .when(stateMachine)
                .transition(anyString(), eq(MessageProcessStatus.COMPLETED));
        BatchResult result = service.process(msg);
        assertThat(result.allSucceeded()).isFalse();
    }

    @Test
    void constructor_shouldRejectNullDependency() {
        assertThatThrownBy(() -> new BatchMessageProcessorService(null, null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    // ── helpers ─────────────────────────────────────

    /**
     * 构造带 msgNo=3007（P2d T1 注册）和 20 位 msgId 的批量 CfxMessage。
     * body 为测试桩 {@link Stub}（带 {@code @XmlRootElement} 以便 JAXB.marshal）。
     */
    private CfxMessage batchOf(final Object... bodies) {
        CommonHead head = new CommonHead();
        head.setMsgNo("3007");
        head.setMsgId("20260423120000000001");
        return CfxMessage.of(head, bodies);
    }

    private static Stub stub(final String v) {
        Stub s = new Stub();
        s.setValue(v);
        return s;
    }

    /**
     * 测试桩：带 {@code @XmlRootElement} 以便 {@code JAXB.marshal} 可序列化，
     * 避免 plain {@code String} body 触发 JAXBException。
     */
    @XmlRootElement(name = "Stub")
    @XmlAccessorType(XmlAccessType.FIELD)
    static class Stub {
        @XmlElement
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
