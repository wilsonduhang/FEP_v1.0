package com.puchain.fep.web.messageinbound;

import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.MsgReturn9020;
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
 * P4-MSG-M T2 — 9020 inbound dispatcher 注册 + {@link MsgReturn9020} SerialNoBearing
 * + 真 {@link XsdValidator} 测试。
 *
 * <p>覆盖三个维度：</p>
 * <ul>
 *   <li><b>registry + SerialNoBearing</b>：{@link InboundMessageDispatcher#bodyTypeRegistry()}
 *       含 9020 → {@link MsgReturn9020}（共 24 entry）；{@code MsgReturn9020 instanceof
 *       SerialNoBearing} 且 {@code getSerialNo()} 恒返回 {@code null}（OriMsgNo 非业务流水号，
 *       fallback transitionNo，镜像 2101/9007）。</li>
 *   <li><b>XSD 合规（真 validator 跑 SUT 产物）</b>：{@code MsgReturn9020} marshal 嵌入完整
 *       CFX envelope（HEAD + MSG + {@code RealHead9020} ResponseHead + body）后用真
 *       {@link XsdValidator} 校验 OriMsgNo MsgNo length=4 facet（正向 "3000" PASS，
 *       负向 "abcd" 非数字 FAIL）。</li>
 *   <li><b>dispatcher routing</b>：{@link InboundMessageDispatcher#dispatch} 接收 9020 valid
 *       envelope → unmarshal {@link MsgReturn9020} → publish
 *       {@link InboundMessageProcessedEvent}，断言 event.type/body class/OriMsgNo。
 *       {@code @MockBean SyncMessageProcessorService} 跳过 XSD/DB pipeline（dispatcher
 *       仍真跑 tryUnmarshalBody + publishEvent），对齐 sibling
 *       {@code Inbound9007And9009WireTest} 测试拓扑。</li>
 * </ul>
 *
 * <p>红线 {@code feedback_xsd_compliance_fix_real_validator_on_sut}：用真 XsdValidator 跑实际
 * marshal 产物，禁 {@code @MockBean} validator。红线
 * {@code feedback_xsd_validator_requires_full_envelope_redline}：schema 根定义在 {@code <CFX>}
 * 元素，必须传完整 envelope，禁直送 body-only fragment。fep-web 不依赖 fep-processor
 * test-jar，无法复用 {@code AbstractXsdValidationTest}，故本类自建
 * {@code new XsdValidator(new XsdSchemaRegistry())}（皆 fep-processor MAIN 类）+ inline
 * {@link #wrapCfx} envelope helper（与 {@code Inbound9007And9009WireTest} 同 envelope 形状）。</p>
 *
 * <p>红线 {@code feedback_registered_inbound_body_must_implement_serialnobearing}：注册到
 * BODY_TYPE_REGISTRY 的 body 必须 implements SerialNoBearing。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class Inbound9020WireTest {

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
    @DisplayName("BODY_TYPE_REGISTRY 含 9020 共 24 entry")
    void bodyTypeRegistryShouldBe24EntriesIncluding9020() {
        assertThat(InboundMessageDispatcher.bodyTypeRegistry()).hasSize(24);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry())
                .containsEntry("9020", MsgReturn9020.class);
    }

    @Test
    @DisplayName("MsgReturn9020 implements SerialNoBearing，getSerialNo() 恒 null")
    void msgReturn9020_shouldImplementSerialNoBearingReturningNull() {
        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("3000");
        assertThat(body).isInstanceOf(SerialNoBearing.class);
        assertThat(((SerialNoBearing) body).getSerialNo())
                .as("OriMsgNo 非业务 SerialNo → null fallback transitionNo（镜像 2101/9007）")
                .isNull();
    }

    @Test
    @DisplayName("9020 OriMsgNo='3000' (MsgNo length=4) → 真 XsdValidator PASS")
    void msgReturn9020_validOriMsgNo_passesXsdValidation() throws Exception {
        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("3000"); // 4-digit number satisfies MsgNo length=4 base=Number

        String bodyXml = marshal(body, MsgReturn9020.class);
        assertThat(bodyXml).contains("<OriMsgNo>3000</OriMsgNo>");

        String envelope = wrap9020(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9020,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9020 valid OriMsgNo errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("9020 OriMsgNo='abcd' (非数字) → 真 XsdValidator FAIL")
    void msgReturn9020_invalidOriMsgNo_failsXsdValidation() throws Exception {
        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("abcd"); // 非数字，违反 MsgNo base=Number

        String bodyXml = marshal(body, MsgReturn9020.class);
        String envelope = wrap9020(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9020,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9020 OriMsgNo='abcd' 非数字必须 fail")
                .isFalse();
    }

    @Test
    @DisplayName("dispatch 9020 → unmarshal MsgReturn9020 + publish event")
    void dispatch_9020_shouldUnmarshalMsgReturn9020AndPublishEvent() {
        final String transitionNo = "00910020";
        mockProcessInboundCompleted(MessageType.MSG_9020, transitionNo);

        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("3000");
        String envelope;
        try {
            envelope = wrap9020(marshal(body, MsgReturn9020.class));
        } catch (final Exception e) {
            throw new IllegalStateException("failed to build MsgReturn9020 envelope", e);
        }
        dispatcher.dispatch("9020", transitionNo, envelope.getBytes(StandardCharsets.UTF_8));

        List<InboundMessageProcessedEvent> captured = eventCollector.snapshot();
        assertThat(captured).as("exactly one event published for 9020").hasSize(1);
        InboundMessageProcessedEvent event = captured.get(0);
        assertThat(event.type()).isEqualTo(MessageType.MSG_9020);
        assertThat(event.transitionNo()).isEqualTo(transitionNo);
        assertThat(event.body()).isInstanceOf(MsgReturn9020.class);
        assertThat(((MsgReturn9020) event.body()).getOriMsgNo()).isEqualTo("3000");
    }

    private void mockProcessInboundCompleted(final MessageType type, final String transitionNo) {
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-it-" + type.msgNo() + "-001abcdef0123456789abcdef0123",
                        type, transitionNo, Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(type), eq(transitionNo), any(byte[].class)))
                .thenReturn(completed);
    }

    private static String wrap9020(final String bodyXml) {
        return wrapCfx("9020",
                "90200000000000000001", "90000000000000000001", """
                <RealHead9020>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260601</EntrustDate>
                  <TransitionNo>00910020</TransitionNo>
                  <Result>90000</Result>
                </RealHead9020>
                """ + bodyXml);
    }

    /**
     * Inline CFX envelope helper (replaces fep-processor test-scope
     * {@code AbstractXsdValidationTest.wrapCfxTemplate}, not visible to fep-web).
     * Builds HEAD (8 mandatory Base.xsd children, Version hard-coded 1.0) + MSG
     * (caller-supplied RealHead9020 ResponseHead + body element).
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
         * Bean 名显式取 {@code generalResponseEventCollector}（非默认方法名
         * {@code capturedEventCollector}）以避开 sibling
         * {@code Inbound9007And9009WireTest$CapturedEventConfig} /
         * {@code InboundListenerWireTest$CapturedEventConfig} 同名 bean 在共享
         * Spring context 下的 {@code BeanDefinitionOverrideException}。
         *
         * @return 同步事件收集器
         */
        @Bean
        CapturedEventCollector generalResponseEventCollector() {
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
