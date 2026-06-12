package com.puchain.fep.web.collector.it;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.mapper.ArchiveInfo3102FieldMapper;
import com.puchain.fep.collector.assembler.mapper.ContractInfo3101FieldMapper;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdSchemaRegistry;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Per-mapper XSD validate gate（Plan §T7b §6b，红线 {@code feedback_xsd_validation_gap}）。
 *
 * <p>本测试验证 collector 的两个完整 mapper（{@link ContractInfo3101FieldMapper} +
 * {@link ArchiveInfo3102FieldMapper}）产出的 body 在按 §6c wire-shape matrix 包入 CFX
 * envelope 后能通过对应 XSD 校验。
 *
 * <p><b>位置：</b>测试位于 fep-web 而非 fep-collector ——避免触发
 * {@code CollectorArchitectureTest R2}（collector 不依赖 converter）。fep-web 同时依赖
 * collector / processor / converter，是唯一能 cross-import 的模块。
 *
 * <p><b>类注解：</b>plain JUnit 5（{@link Test} only），不用 {@code @SpringBootTest}
 * （参 fep-processor {@code XsdValidatorTest} 既有惯例：手工 {@code new XsdValidator(new XsdSchemaRegistry())}）。
 *
 * <p><b>装配策略：</b>HEAD/HEAD-element/CFX 外壳采用字符串模板（与
 * {@code SupplyChainXsdValidationTest} 同款），mapper body 用 JAXB marshal 后插入。
 * 这绕开 {@link com.puchain.fep.converter.model.ResponseBusinessHead} 继承
 * {@code RequestBusinessHead} 时 JAXB 在
 * {@code @XmlType.propOrder} 上的继承冲突（JAXB 不允许子类的 propOrder 引用父类继承的
 * properties），同时仍然行使了 mapper → XSD 完整通路。
 *
 * <p><b>覆盖范围（T7b）：</b>仅 3101 + 3102。其他 6 个 stub mapper 的 XSD validate gate
 * 列入 Plan §T7b Deferred D8 ticket pool。
 */
class Per3101AndPer3102EnvelopeXsdValidateTest {

    private static final String INSTITUTION_CODE = "12345678901234";
    private static final String HNDEMP_CENTER = FepConstants.HNDEMP_NODE_CODE;

    /** Common HEAD shared by 3101 / 3102 (msgNo placeholder). */
    private static final String HEAD_TEMPLATE = """
            <HEAD>
                <Version>1.0</Version>
                <SrcNode>12345678901234</SrcNode>
                <DesNode>%s</DesNode>
                <App>HNDEMP</App>
                <MsgNo>{{MSG_NO}}</MsgNo>
                <MsgId>20260501120000000001</MsgId>
                <CorrMsgId>20260501120000000001</CorrMsgId>
                <WorkDate>20260501</WorkDate>
            </HEAD>""".formatted(FepConstants.HNDEMP_NODE_CODE);

    /** RequestHead body (3 fields) — used by 3102/3105/3107/3109/3112/3116/3009. */
    private static final String REQUEST_HEAD_FIELDS = """
                <SendOrgCode>12345678901234</SendOrgCode>
                <EntrustDate>20260501</EntrustDate>
                <TransitionNo>00000001</TransitionNo>""";

    /** ResponseHead body (5 fields incl. Result) — only 3101 in §T7b scope. */
    private static final String RESPONSE_HEAD_FIELDS = """
                <SendOrgCode>12345678901234</SendOrgCode>
                <EntrustDate>20260501</EntrustDate>
                <TransitionNo>00000001</TransitionNo>
                <Result>90000</Result>""";

    private static XsdValidator validator;
    private static ContractInfo3101FieldMapper contract3101Mapper;
    private static ArchiveInfo3102FieldMapper archive3102Mapper;
    private static JAXBContext bodyJaxbCtx3101;
    private static JAXBContext bodyJaxbCtx3102;

    @BeforeAll
    static void init() throws JAXBException {
        validator = new XsdValidator(new XsdSchemaRegistry());
        final CollectorProperties props = new CollectorProperties();
        props.setInstitutionCode(INSTITUTION_CODE);
        contract3101Mapper = new ContractInfo3101FieldMapper(props);
        archive3102Mapper = new ArchiveInfo3102FieldMapper(props);
        bodyJaxbCtx3101 = JAXBContext.newInstance(ContractInfo3101.class);
        bodyJaxbCtx3102 = JAXBContext.newInstance(ArchiveInfo3102.class);
    }

    @Test
    void shouldPassXsdValidation_for3101Mapper() throws Exception {
        final Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN2026050100000000000000000001"); // 30 chars
        raw.put("contract_no", "HT202605010001");
        raw.put("contract_type", "01");
        raw.put("digital_seal", "1");
        raw.put("contract_filename", "contract.pdf");
        raw.put("jfqy_name", "甲方企业");
        raw.put("yfqy_name", "乙方企业");
        raw.put("hxqy_code", "913201000000000001");

        final ContractInfo3101 body = (ContractInfo3101) contract3101Mapper.toMessageBody(raw);
        final String bodyXml = marshalBodyFragment(bodyJaxbCtx3101, body);

        // 3101 BatchHead uses ResponseHead (§6c) — 5 fields incl. Result
        final String envelope = cfx("3101", """
                <BatchHead3101>
        """.formatted(FepConstants.HNDEMP_NODE_CODE) + RESPONSE_HEAD_FIELDS + """
                </BatchHead3101>
            """ + bodyXml);

        final ValidationResult result = validator.validate(MessageType.MSG_3101,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("XSD errors: %s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldPassXsdValidation_for3102Mapper() throws Exception {
        final Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN2026050100000000000000000002"); // 30 chars
        raw.put("apply_mode", "1");
        raw.put("hxqy_name", "核心企业A");
        raw.put("hxqy_code", "913201000000000001");
        raw.put("rzqy_name", "融资企业B");
        raw.put("rzqy_code", "913201000000000002");

        final ArchiveInfo3102 body = (ArchiveInfo3102) archive3102Mapper.toMessageBody(raw);
        final String bodyXml = marshalBodyFragment(bodyJaxbCtx3102, body);

        // 3102 BatchHead uses RequestHead (§6c) — 3 fields, no Result
        final String envelope = cfx("3102", """
                <BatchHead3102>
            """ + REQUEST_HEAD_FIELDS + """
                </BatchHead3102>
            """ + bodyXml);

        final ValidationResult result = validator.validate(MessageType.MSG_3102,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("XSD errors: %s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    /**
     * Marshal a body POJO as a fragment (no XML declaration, ready to splice into CFX).
     */
    private static String marshalBodyFragment(final JAXBContext ctx, final Object body)
            throws JAXBException {
        final Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        final StringWriter sw = new StringWriter();
        m.marshal(body, sw);
        return sw.toString();
    }

    /**
     * Wrap MSG content into a complete CFX envelope.
     *
     * @param msgNo      4-digit message number
     * @param msgContent XML content inside {@code <MSG>}
     * @return complete CFX XML string
     */
    private static String cfx(final String msgNo, final String msgContent) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<CFX>\n"
                + HEAD_TEMPLATE.replace("{{MSG_NO}}", msgNo) + "\n"
                + "    <MSG>\n" + msgContent + "\n    </MSG>\n</CFX>";
    }
}
