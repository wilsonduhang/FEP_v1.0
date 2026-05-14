package com.puchain.fep.processor.pipeline;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.InMemoryMessageProcessStore;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.XsdValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PRD v1.3 §4.7 同步模式端到端集成测试。
 *
 * <p>覆盖 10 份合法样本（1001/2001/1004/2004/3001/3002/3003/3005/3006/9005）的成功路径，
 * 3 份非法样本（missing company name / invalid date / missing serial）的失败路径，
 * 以及混合批次下 transitionNo 隔离的状态追踪。3004 正样本因 complexType 嵌套过深延至 P2b。</p>
 *
 * <p>类名以 {@code IntegrationTest} 结尾，沿用 P1b {@code MessagePipelineIntegrationTest}
 * 约定，由 Surefire 在 {@code test} 阶段执行（本仓库未配置 Failsafe 插件）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SyncMessageProcessorServiceIntegrationTest {

    private SyncMessageProcessorService processor;
    private InMemoryMessageProcessStore store;

    private static final Map<String, MessageType> VALID_SAMPLES = new LinkedHashMap<>();
    static {
        VALID_SAMPLES.put("1001-valid.xml", MessageType.MSG_1001);
        VALID_SAMPLES.put("2001-valid.xml", MessageType.MSG_2001);
        VALID_SAMPLES.put("1004-valid.xml", MessageType.MSG_1004);
        VALID_SAMPLES.put("2004-valid.xml", MessageType.MSG_2004);
        VALID_SAMPLES.put("3001-valid.xml", MessageType.MSG_3001);
        VALID_SAMPLES.put("3002-valid.xml", MessageType.MSG_3002);
        VALID_SAMPLES.put("3003-valid.xml", MessageType.MSG_3003);
        VALID_SAMPLES.put("3005-valid.xml", MessageType.MSG_3005);
        VALID_SAMPLES.put("3006-valid.xml", MessageType.MSG_3006);
        VALID_SAMPLES.put("9005-valid.xml", MessageType.MSG_9005);
    }

    private static final List<InvalidCase> INVALID_SAMPLES = List.of(
            new InvalidCase("1001-missing-company-name.xml", MessageType.MSG_1001),
            new InvalidCase("1001-invalid-date.xml", MessageType.MSG_1001),
            new InvalidCase("3001-missing-serial.xml", MessageType.MSG_3001)
    );

    private record InvalidCase(String file, MessageType type) {
    }

    @BeforeEach
    void setUp() {
        XsdValidator validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
        store = new InMemoryMessageProcessStore();
        MessageStateMachine machine = new MessageStateMachine(store);
        processor = new SyncMessageProcessorService(validator, machine, store);
    }

    /**
     * 从 test resources 加载报文样本文件为字节数组。
     *
     * @param name 样本文件名（自动加 {@code /samples/} 前缀，形如 {@code 1001-valid.xml}）
     * @return 样本文件完整字节内容
     * @throws IOException 当资源不存在或读取失败
     */
    private byte[] loadSample(final String name) throws IOException {
        try (InputStream is = SyncMessageProcessorServiceIntegrationTest.class.getResourceAsStream("/samples/" + name)) {
            if (is == null) {
                throw new IOException("missing sample: " + name);
            }
            return is.readAllBytes();
        }
    }

    @Test
    void tenValidSamples_shouldReachCompleted() throws IOException {
        Instant start = Instant.now();
        int idx = 0;
        for (Map.Entry<String, MessageType> entry : VALID_SAMPLES.entrySet()) {
            byte[] xml = loadSample(entry.getKey());
            MessageProcessRecord result = processor.processOutbound(
                    entry.getValue(), "TX-IT-OK-" + (idx++), xml);
            assertThat(result.getStatus())
                    .as("sample %s should reach COMPLETED", entry.getKey())
                    .isEqualTo(MessageProcessStatus.COMPLETED);
        }
        Duration elapsed = Duration.between(start, Instant.now());
        assertThat(elapsed).as("10 samples within 3 seconds").isLessThan(Duration.ofSeconds(3));
        assertThat(store.countByStatus(MessageProcessStatus.COMPLETED)).isEqualTo(10);
    }

    @Test
    void invalidSamples_shouldFailWithProc8501() throws IOException {
        int idx = 0;
        for (InvalidCase invalid : INVALID_SAMPLES) {
            byte[] xml = loadSample(invalid.file());
            MessageProcessRecord result = processor.processOutbound(
                    invalid.type(), "TX-IT-FAIL-" + (idx++), xml);
            assertThat(result.getStatus())
                    .as("sample %s should FAIL", invalid.file())
                    .isEqualTo(MessageProcessStatus.FAILED);
            assertThat(result.getErrorCode()).isEqualTo("PROC_8501");
        }
        assertThat(store.countByStatus(MessageProcessStatus.FAILED)).isEqualTo(3);
    }

    @Test
    void mixedBatch_shouldTrackStatesIndependently() throws IOException {
        processor.processOutbound(MessageType.MSG_1001, "TX-MIX-1",
                loadSample("1001-valid.xml"));
        processor.processOutbound(MessageType.MSG_1001, "TX-MIX-2",
                loadSample("1001-missing-company-name.xml"));
        processor.processOutbound(MessageType.MSG_9005, "TX-MIX-3",
                loadSample("9005-valid.xml"));

        assertThat(store.countByStatus(MessageProcessStatus.COMPLETED)).isEqualTo(2);
        assertThat(store.countByStatus(MessageProcessStatus.FAILED)).isEqualTo(1);
    }
}
