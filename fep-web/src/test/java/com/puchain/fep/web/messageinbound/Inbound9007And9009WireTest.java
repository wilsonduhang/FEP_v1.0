package com.puchain.fep.web.messageinbound;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.LoginResponse9007;
import com.puchain.fep.processor.body.common.LogoutResponse9009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.pipeline.SyncMessageProcessorService;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdSchemaRegistry;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestPropertySource;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * P4-MSG-L T2 — 9007/9009 inbound dispatcher 注册 + body POJO + 真 {@link XsdValidator} 测试。
 *
 * <p>覆盖两个维度：</p>
 * <ul>
 *   <li><b>XSD 合规（真 validator 跑 SUT 产物）</b>：{@code LoginResponse9007} /
 *       {@code LogoutResponse9009} marshal 嵌入完整 CFX envelope（HEAD + MSG +
 *       {@code RealHead{msgNo}} ResponseHead + body）后，用真 {@link XsdValidator}
 *       校验 {@code NodeStatus} 字段 maxLength=2 facet（正向 "01" 2 chars PASS，
 *       负向 "123" 3 chars FAIL 含 "maxLength"）。</li>
 *   <li><b>dispatcher routing</b>：{@link InboundMessageDispatcher#dispatch} 接收
 *       9007/9009 valid envelope → unmarshal 对应 body POJO → publish
 *       {@link InboundMessageProcessedEvent}，断言 event.type/body class。
 *       {@code @MockBean SyncMessageProcessorService} 跳过 XSD/DB pipeline（dispatcher
 *       仍真跑 tryUnmarshalBody + publishEvent），对齐 sibling
 *       {@code InboundListenerWireTest} 测试拓扑。</li>
 * </ul>
 *
 * <p>红线 {@code feedback_xsd_compliance_fix_real_validator_on_sut}：用真 XsdValidator 跑实际
 * marshal 产物，禁 {@code @MockBean} validator（dispatch-routing 维度 mock 的是
 * syncProcessor 而非 validator）。红线 {@code feedback_xsd_validator_requires_full_envelope_redline}：
 * {@code XsdValidator.validate} 的 schema 根定义在 {@code <CFX>} 元素，必须传完整 envelope
 * （HEAD + MSG + RealHead{msgNo} + body），禁直送 body-only fragment。fep-web 不依赖
 * fep-processor test-jar，无法复用 {@code AbstractXsdValidationTest.wrapCfxTemplate} /
 * {@code SHARED_VALIDATOR}（test-scope），故本类自建 {@code new XsdValidator(new
 * XsdSchemaRegistry())}（皆 fep-processor MAIN 类）+ inline {@link #wrapCfx} envelope helper
 * （与 T1 {@code OutboundWireShape9006And9008XsdComplianceTest} 同 envelope 形状）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class Inbound9007And9009WireTest {

    private static final String SRC_NODE = "A1000143000104";
    private static final String DES_NODE = "12345678901234";
    private static final String APP = "HNDEMP";

    @Autowired
    private InboundMessageDispatcher dispatcher;

    @MockBean
    private SyncMessageProcessorService syncProcessor;

    @Autowired
    private CapturedEventCollector eventCollector;

    private XsdValidator validator;

    @BeforeEach
    void setUp() {
        Mockito.reset(syncProcessor);
        eventCollector.clear();
        validator = new XsdValidator(new XsdSchemaRegistry());
    }

    @Test
    @DisplayName("BODY_TYPE_REGISTRY 含 9007/9009 共 24 entry（P4-MSG-M 扩展 9020 后）")
    void bodyTypeRegistryShouldBe24Entries() {
        assertThat(InboundMessageDispatcher.bodyTypeRegistry()).hasSize(24);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry())
                .containsEntry("9007", LoginResponse9007.class)
                .containsEntry("9009", LogoutResponse9009.class);
    }

    @Test
    @DisplayName("9007 Status='99' (2 chars) → 真 XsdValidator PASS")
    void loginResponse9007_validStatus_passesXsdValidation() throws Exception {
        LoginResponse9007 body = new LoginResponse9007();
        body.setStatus("99"); // 2 chars satisfies NodeStatus minLength=1 maxLength=2

        String bodyXml = marshal(body, LoginResponse9007.class);
        assertThat(bodyXml).contains("<Status>99</Status>");

        String envelope = wrap9007(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9007,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9007 valid Status errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("9007 Status='123' (3 chars > maxLength=2) → 真 XsdValidator FAIL 含 maxLength")
    void loginResponse9007_statusTooLong_failsXsdValidation() throws Exception {
        LoginResponse9007 body = new LoginResponse9007();
        body.setStatus("123"); // 3 chars > NodeStatus maxLength=2

        String bodyXml = marshal(body, LoginResponse9007.class);
        String envelope = wrap9007(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9007,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9007 Status='123' (3>2) must fail XSD maxLength")
                .isFalse();
        assertThat(result.errors())
                .anyMatch(e -> e.contains("maxLength"));
    }

    @Test
    @DisplayName("9009 Status='99' (2 chars) → 真 XsdValidator PASS")
    void logoutResponse9009_validStatus_passesXsdValidation() throws Exception {
        LogoutResponse9009 body = new LogoutResponse9009();
        body.setStatus("99");

        String bodyXml = marshal(body, LogoutResponse9009.class);
        assertThat(bodyXml).contains("<Status>99</Status>");

        String envelope = wrap9009(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9009,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9009 valid Status errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("9009 Status='123' (3 chars > maxLength=2) → 真 XsdValidator FAIL 含 maxLength")
    void logoutResponse9009_statusTooLong_failsXsdValidation() throws Exception {
        LogoutResponse9009 body = new LogoutResponse9009();
        body.setStatus("123");

        String bodyXml = marshal(body, LogoutResponse9009.class);
        String envelope = wrap9009(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9009,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9009 Status='123' (3>2) must fail XSD maxLength")
                .isFalse();
        assertThat(result.errors())
                .anyMatch(e -> e.contains("maxLength"));
    }

    @Test
    @DisplayName("dispatch 9007 → unmarshal LoginResponse9007 + publish event")
    void dispatch_9007_shouldUnmarshalLoginResponse9007AndPublishEvent() {
        final String transitionNo = "00910007";
        mockProcessInboundCompleted(MessageType.MSG_9007, transitionNo);

        String envelope = wrap9007(marshalStatus(LoginResponse9007.class, "99"));
        dispatcher.dispatch("9007", transitionNo, envelope.getBytes(StandardCharsets.UTF_8));

        List<InboundMessageProcessedEvent> captured = eventCollector.snapshot();
        assertThat(captured).as("exactly one event published for 9007").hasSize(1);
        InboundMessageProcessedEvent event = captured.get(0);
        assertThat(event.type()).isEqualTo(MessageType.MSG_9007);
        assertThat(event.transitionNo()).isEqualTo(transitionNo);
        assertThat(event.body()).isInstanceOf(LoginResponse9007.class);
        assertThat(((LoginResponse9007) event.body()).getStatus()).isEqualTo("99");
    }

    @Test
    @DisplayName("dispatch 9009 → unmarshal LogoutResponse9009 + publish event")
    void dispatch_9009_shouldUnmarshalLogoutResponse9009AndPublishEvent() {
        final String transitionNo = "00910009";
        mockProcessInboundCompleted(MessageType.MSG_9009, transitionNo);

        String envelope = wrap9009(marshalStatus(LogoutResponse9009.class, "99"));
        dispatcher.dispatch("9009", transitionNo, envelope.getBytes(StandardCharsets.UTF_8));

        List<InboundMessageProcessedEvent> captured = eventCollector.snapshot();
        assertThat(captured).as("exactly one event published for 9009").hasSize(1);
        InboundMessageProcessedEvent event = captured.get(0);
        assertThat(event.type()).isEqualTo(MessageType.MSG_9009);
        assertThat(event.transitionNo()).isEqualTo(transitionNo);
        assertThat(event.body()).isInstanceOf(LogoutResponse9009.class);
        assertThat(((LogoutResponse9009) event.body()).getStatus()).isEqualTo("99");
    }

    private void mockProcessInboundCompleted(final MessageType type, final String transitionNo) {
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-it-" + type.msgNo() + "-001abcdef0123456789abcdef0123",
                        type, transitionNo, Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(type), eq(transitionNo), any(byte[].class)))
                .thenReturn(completed);
    }

    private static String wrap9007(final String bodyXml) {
        return wrapCfx("9007",
                "90070000000000000001", "90060000000000000001", """
                <RealHead9007>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260601</EntrustDate>
                  <TransitionNo>00910007</TransitionNo>
                  <Result>90000</Result>
                </RealHead9007>
                """ + bodyXml);
    }

    private static String wrap9009(final String bodyXml) {
        return wrapCfx("9009",
                "90090000000000000001", "90080000000000000001", """
                <RealHead9009>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260601</EntrustDate>
                  <TransitionNo>00910009</TransitionNo>
                  <Result>90000</Result>
                </RealHead9009>
                """ + bodyXml);
    }

    /**
     * Inline CFX envelope helper (replaces fep-processor test-scope
     * {@code AbstractXsdValidationTest.wrapCfxTemplate}, not visible to fep-web).
     * Builds HEAD (8 mandatory Base.xsd children, Version hard-coded 1.0) + MSG
     * (caller-supplied RealHead{msgNo} ResponseHead + body element).
     *
     * @param msgNo 4-digit message number
     * @param msgId full 20-digit HEAD MsgId
     * @param corrMsgId full 20-digit HEAD CorrMsgId
     * @param msgInnerXml complete inner XML of {@code MSG} (RealHead + body)
     * @return complete CFX envelope XML
     */
    private static String wrapCfx(final String msgNo, final String msgId,
                                  final String corrMsgId, final String msgInnerXml) {
        return ("""
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>%s</SrcNode>
                    <DesNode>%s</DesNode>
                    <App>%s</App>
                    <MsgNo>%s</MsgNo>
                    <MsgId>%s</MsgId>
                    <CorrMsgId>%s</CorrMsgId>
                    <WorkDate>20260601</WorkDate>
                  </HEAD>
                  <MSG>
                %s
                  </MSG>
                </CFX>
                """).formatted(SRC_NODE, DES_NODE, APP, msgNo, msgId, corrMsgId, msgInnerXml);
    }

    private static <T> String marshalStatus(final Class<T> bodyClass, final String status) {
        try {
            final T body = bodyClass.getDeclaredConstructor().newInstance();
            bodyClass.getMethod("setStatus", String.class).invoke(body, status);
            return marshal(body, bodyClass);
        } catch (final Exception e) {
            throw new IllegalStateException("failed to build " + bodyClass.getSimpleName(), e);
        }
    }

    private static <T> String marshal(final T body, final Class<T> bodyClass) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(bodyClass);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter sw = new StringWriter();
        m.marshal(body, sw);
        return sw.toString();
    }

    /**
     * Spring {@code @TestConfiguration} 注册同步事件收集器 bean（与 dispatcher
     * publishEvent 同事务，无需 await）。
     */
    @TestConfiguration
    static class CapturedEventConfig {

        /**
         * Bean 名显式取 {@code nodeLifecycleEventCollector}（非默认方法名
         * {@code capturedEventCollector}）以避开 sibling
         * {@code InboundListenerWireTest$CapturedEventConfig} 同名 bean 在共享
         * Spring context 下的 {@code BeanDefinitionOverrideException}。
         *
         * @return 同步事件收集器
         */
        @Bean
        CapturedEventCollector nodeLifecycleEventCollector() {
            return new CapturedEventCollector();
        }
    }

    /**
     * 线程安全事件收集器；{@code snapshot} 返回 List 副本防御断言期被改。
     */
    static class CapturedEventCollector {

        private final List<InboundMessageProcessedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void onInboundProcessed(final InboundMessageProcessedEvent event) {
            events.add(event);
        }

        public void clear() {
            events.clear();
        }

        public List<InboundMessageProcessedEvent> snapshot() {
            return List.copyOf(events);
        }
    }
}
