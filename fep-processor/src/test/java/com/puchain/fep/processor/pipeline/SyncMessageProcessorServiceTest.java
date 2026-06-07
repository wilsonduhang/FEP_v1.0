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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SyncMessageProcessorService} — synchronous message
 * pipeline coordinating XSD validation, state machine transitions, and process
 * record persistence.
 *
 * <p>Coverage:</p>
 * <ul>
 *     <li>Outbound: valid message completes; invalid XML fails with {@link com.puchain.fep.common.exception.FepBusinessException}</li>
 *     <li>Inbound: valid message completes</li>
 *     <li>Deduplication: duplicate {@code transitionNo} is rejected</li>
 *     <li>Defensive: null inputs are rejected</li>
 *     <li>State store: process record is created in the {@link com.puchain.fep.processor.state.InMemoryMessageProcessStore}</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SyncMessageProcessorServiceTest {

    // E-NIT-7: registry + validator promoted to module-level static singletons
    // on {@link AbstractXsdValidationTest}. XsdSchemaRegistry eager-compiles
    // 44 XSDs (~hundreds of ms) with an immutable cache (Map.copyOf) — sharing
    // the registry amortizes this eager-load cost across all 6 tests in this
    // class and across the entire fep-processor module (Surefire default
    // forkCount=1 reuseForks=true → single JVM per module), eliminating
    // 264+ redundant schema builds module-wide.
    //
    // XsdValidator is stateless (each validate() call creates a fresh
    // CollectingErrorHandler) — sharing the validator instance is incidental
    // and yields no measurable timing benefit, only convenience. The real win
    // is registry sharing.

    private SyncMessageProcessorService processor;
    private InMemoryMessageProcessStore store;
    private com.puchain.fep.processor.validation.rule.MessageRuleRegistry ruleRegistry;

    @BeforeEach
    void setUp() {
        // store is stateful (ConcurrentMap) — the duplicate-transitionNo test
        // requires a clean instance per case, so machine/processor rebuild too.
        XsdValidator validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
        store = new InMemoryMessageProcessStore();
        MessageStateMachine machine = new MessageStateMachine(store);
        ruleRegistry = new com.puchain.fep.processor.validation.rule.MessageRuleRegistry();
        com.puchain.fep.processor.validation.BusinessRuleValidator businessRuleValidator =
                new com.puchain.fep.processor.validation.BusinessRuleValidator(ruleRegistry);
        processor = new SyncMessageProcessorService(validator, businessRuleValidator, machine, store);
    }

    /**
     * 从 test resources 加载报文样本文件为字节数组。
     *
     * @param path classpath 资源路径（相对 test/resources，形如 {@code /samples/1001-valid.xml}）
     * @return 样本文件完整字节内容
     * @throws IOException 当资源不存在或读取失败
     */
    private byte[] loadSample(final String path) throws IOException {
        try (InputStream is = SyncMessageProcessorServiceTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("sample missing: " + path);
            }
            return is.readAllBytes();
        }
    }

    @Test
    void processOutbound_shouldCompleteValidMessage() throws IOException {
        byte[] xml = loadSample("/samples/1001-valid.xml");
        MessageProcessRecord result = processor.processOutbound(
                MessageType.MSG_1001, "TX20260411000001", xml);
        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void processOutbound_shouldFailOnInvalidXml() throws IOException {
        byte[] xml = loadSample("/samples/1001-missing-company-name.xml");
        MessageProcessRecord result = processor.processOutbound(
                MessageType.MSG_1001, "TX20260411000002", xml);
        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(result.getErrorCode()).isEqualTo("PROC_8501");
        assertThat(result.getErrorMessage()).containsIgnoringCase("CompanyName");
    }

    @Test
    void processInbound_shouldCompleteValidMessage() throws IOException {
        byte[] xml = loadSample("/samples/1001-valid.xml");
        MessageProcessRecord result = processor.processInbound(
                MessageType.MSG_1001, "TX20260411000003", xml);
        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
    }

    @Test
    void process_shouldRejectDuplicateTransitionNo() throws IOException {
        byte[] xml = loadSample("/samples/1001-valid.xml");
        processor.processOutbound(MessageType.MSG_1001, "TX-DUP", xml);
        assertThatThrownBy(() -> processor.processOutbound(MessageType.MSG_1001, "TX-DUP", xml))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TX-DUP");
    }

    @Test
    void process_shouldRejectNullInputs() throws IOException {
        byte[] xml = loadSample("/samples/1001-valid.xml");
        assertThatThrownBy(() -> processor.processOutbound(null, "TX-N1", xml))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.processOutbound(MessageType.MSG_1001, null, xml))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.processOutbound(MessageType.MSG_1001, "TX-N3", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // process_shouldRejectUnsupportedMessageType removed: P4-MSG-D wired MSG_1101
    // through XsdSchemaRegistry, so the previously-unsupported branch is no
    // longer reachable through MSG_1101 (or any other MessageType — all 44 enum
    // values are now mapped). The defensive throw inside the processor remains
    // but cannot be exercised through the type system without reflection.

    @Test
    void process_shouldCreateRecordInStore() throws IOException {
        byte[] xml = loadSample("/samples/1001-valid.xml");
        processor.processOutbound(MessageType.MSG_1001, "TX-STORED", xml);
        assertThat(store.findByTransitionNo("TX-STORED"))
                .isPresent()
                .get()
                .extracting(MessageProcessRecord::getStatus)
                .isEqualTo(MessageProcessStatus.COMPLETED);
    }

    @Test
    void process_shouldFailWithProc8507_whenBusinessRuleViolated() throws IOException {
        // XSD-valid sample but a registered business rule fails → FAILED(PROC_8507),
        // proving the second (business) gate runs after XSD passes.
        byte[] xml = loadSample("/samples/1001-valid.xml");
        ruleRegistry.register(MessageType.MSG_1001,
                ctx -> java.util.Optional.of("forced business violation for test"));

        MessageProcessRecord result = processor.processInbound(
                MessageType.MSG_1001, "TX-RULE-FAIL", xml);

        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(result.getErrorCode()).isEqualTo("PROC_8507");
        assertThat(result.getErrorMessage()).contains("forced business violation");
    }

    @Test
    void process_shouldComplete_whenBusinessRulesPass() throws IOException {
        // XSD-valid sample + a registered rule that passes → COMPLETED (backward compatible).
        byte[] xml = loadSample("/samples/1001-valid.xml");
        ruleRegistry.register(MessageType.MSG_1001, ctx -> java.util.Optional.empty());

        MessageProcessRecord result = processor.processInbound(
                MessageType.MSG_1001, "TX-RULE-PASS", xml);

        assertThat(result.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
        assertThat(result.getErrorCode()).isNull();
    }
}
