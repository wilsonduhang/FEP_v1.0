package com.puchain.fep.processor.pipeline;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.InMemoryMessageProcessStore;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.XsdSchemaRegistry;
import com.puchain.fep.processor.validation.XsdValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncMessageProcessorServiceTest {

    private AsyncMessageProcessorService processor;
    private InMemoryMessageProcessStore store;

    @BeforeEach
    void setUp() {
        XsdSchemaRegistry registry = new XsdSchemaRegistry();
        XsdValidator validator = new XsdValidator(registry);
        store = new InMemoryMessageProcessStore();
        MessageStateMachine machine = new MessageStateMachine(store);
        processor = new AsyncMessageProcessorService(validator, machine, store);
    }

    /**
     * 从 test resources 加载报文样本文件为字节数组。
     *
     * @param path classpath 资源路径（相对 test/resources，形如 {@code /samples/3001-valid.xml}）
     * @return 样本文件完整字节内容
     * @throws IOException 当资源不存在或读取失败
     */
    private byte[] loadSample(final String path) throws IOException {
        try (InputStream is = AsyncMessageProcessorServiceTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("sample missing: " + path);
            }
            return is.readAllBytes();
        }
    }

    @Test
    void processAsyncInbound_validXml_shouldReturnProcessingRecord() throws IOException {
        byte[] xml = loadSample("/samples/3001-valid.xml");
        MessageProcessRecord result = processor.processAsyncInbound(
                MessageType.MSG_3001, "TX20260411000001", xml);
        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.PROCESSING);
        assertThat(result.getErrorCode()).isNull();
        assertThat(result.getMessageType()).isEqualTo(MessageType.MSG_3001);
        assertThat(result.getTransitionNo()).isEqualTo("TX20260411000001");
    }

    @Test
    void processAsyncOutbound_validXml_shouldReturnProcessingRecord() throws IOException {
        byte[] xml = loadSample("/samples/3001-valid.xml");
        MessageProcessRecord result = processor.processAsyncOutbound(
                MessageType.MSG_3001, "TX20260411000002", xml);
        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.PROCESSING);
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void completeWithResponse_afterInbound_shouldReturnCompleted() throws IOException {
        byte[] requestXml = loadSample("/samples/3001-valid.xml");
        processor.processAsyncInbound(
                MessageType.MSG_3001, "TX20260411000003", requestXml);

        byte[] responseXml = loadSample("/samples/3002-valid.xml");
        MessageProcessRecord completed = processor.completeWithResponse(
                "TX20260411000003", MessageType.MSG_3002, responseXml);

        assertThat(completed.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
        assertThat(completed.getErrorCode()).isNull();
        assertThat(completed.getTransitionNo()).isEqualTo("TX20260411000003");
    }

    @Test
    void processAsync_invalidXml_shouldReturnFailed() {
        byte[] invalidXml = "<CFX><HEAD><Version>1.0</Version></HEAD></CFX>".getBytes();
        MessageProcessRecord result = processor.processAsyncInbound(
                MessageType.MSG_3001, "TX20260411000004", invalidXml);
        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(result.getErrorCode()).isEqualTo("PROC_8501");
    }

    @Test
    void processAsync_duplicateTransitionNo_shouldThrow() throws IOException {
        byte[] xml = loadSample("/samples/3001-valid.xml");
        processor.processAsyncInbound(MessageType.MSG_3001, "TX-DUP-ASYNC", xml);
        assertThatThrownBy(() -> processor.processAsyncInbound(
                MessageType.MSG_3001, "TX-DUP-ASYNC", xml))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TX-DUP-ASYNC");
    }

    @Test
    void processAsync_nullType_shouldThrow() throws IOException {
        byte[] xml = loadSample("/samples/3001-valid.xml");
        assertThatThrownBy(() -> processor.processAsyncInbound(null, "TX-N1", xml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void processAsync_nullTransitionNo_shouldThrow() throws IOException {
        byte[] xml = loadSample("/samples/3001-valid.xml");
        assertThatThrownBy(() -> processor.processAsyncInbound(
                MessageType.MSG_3001, null, xml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transitionNo");
    }

    @Test
    void processAsync_nullXml_shouldThrow() {
        assertThatThrownBy(() -> processor.processAsyncInbound(
                MessageType.MSG_3001, "TX-N3", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("xml");
    }

    @Test
    void completeWithResponse_unknownTransitionNo_shouldThrow() throws IOException {
        byte[] xml = loadSample("/samples/3002-valid.xml");
        assertThatThrownBy(() -> processor.completeWithResponse(
                "TX-UNKNOWN", MessageType.MSG_3002, xml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TX-UNKNOWN");
    }

    @Test
    void completeWithResponse_notInProcessing_shouldThrow() throws IOException {
        // Create a record that ends in FAILED (via invalid XML)
        byte[] invalidXml = "<CFX><HEAD><Version>1.0</Version></HEAD></CFX>".getBytes();
        processor.processAsyncInbound(
                MessageType.MSG_3001, "TX-ALREADY-FAIL", invalidXml);

        byte[] responseXml = loadSample("/samples/3002-valid.xml");
        assertThatThrownBy(() -> processor.completeWithResponse(
                "TX-ALREADY-FAIL", MessageType.MSG_3002, responseXml))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROCESSING");
    }
}
