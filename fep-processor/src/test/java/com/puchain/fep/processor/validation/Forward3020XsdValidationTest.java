package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3020 (bidirectional Forward) message body.
 *
 * <p>Coverage (P4-MSG-G T4b，对齐 {@link ProgressQuery3001XsdValidationTest} pattern,
 * 复用 {@link AbstractXsdValidationTest#wrapCfxTemplate}) — 4 cases covering the
 * {@code RequestResponseHead} Result/AddWord optional combinations:</p>
 * <ul>
 *     <li>case1 valid: head {@code Result} + {@code AddWord} both omitted
 *         (3020.xsd RealHead3020 type=RequestResponseHead, Base.xsd:144 Result
 *         minOccurs=0 / :149 AddWord minOccurs=0)</li>
 *     <li>case2 valid: head {@code Result} filled (Number length=5) +
 *         {@code AddWord} omitted</li>
 *     <li>case3 valid: head {@code Result} omitted + {@code AddWord} filled
 *         (Text maxLen=200)</li>
 *     <li>case4 invalid: body {@code SerialNo} missing
 *         ({@link com.puchain.fep.processor.body.supplychain.Forward3020}
 *         {@code @XmlElement(name="SerialNo", required=true)} +
 *         3020.xsd Forward3020 sequence requires SerialNo)</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: SerialNo(Text length=30) /
 * NodeCode(SrcNodeCode/DesNodeCode String length=14) /
 * OrgCode(SendOrgCode String length=14) / TransitionNo(Number length=8) /
 * Result(Number length=5) / AddWord(Text maxLen=200) /
 * BusinessNo(Text minLen=1, maxLen=20).</p>
 *
 * <p>m3 互补: {@link SupplyChainExtXsdValidationTest} 从 classpath
 * {@code /samples/3020-valid.xml} 加载 1 valid sample 校验整体可达性；本测试用
 * 内联 fixture 覆盖 RequestResponseHead Result/AddWord 必/可选结构变体（4 case），
 * 两者互补不重复。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class Forward3020XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_RESULT_NULL_ADDWORD_NULL_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "3020",
            "30200000000000000001", "00000000000000000000", "20260514", """
                <RealHead3020>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </RealHead3020>
                <Forward3020>
                  <SerialNo>SN302000000000000000000000001A</SerialNo>
                  <SrcNodeCode>A1000142000001</SrcNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <BusinessNo>BIZ001</BusinessNo>
                  <Parameters>k=v</Parameters>
                  <Content>payload</Content>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </Forward3020>""");

    private static final String VALID_RESULT_FILLED_ADDWORD_NULL_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "3020",
            "30200000000000000002", "00000000000000000000", "20260514", """
                <RealHead3020>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </RealHead3020>
                <Forward3020>
                  <SerialNo>SN302000000000000000000000002A</SerialNo>
                  <SrcNodeCode>A1000142000001</SrcNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                </Forward3020>""");

    private static final String VALID_RESULT_NULL_ADDWORD_FILLED_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "3020",
            "30200000000000000003", "00000000000000000000", "20260514", """
                <RealHead3020>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <AddWord>转发处理中，无业务结果码</AddWord>
                </RealHead3020>
                <Forward3020>
                  <SerialNo>SN302000000000000000000000003A</SerialNo>
                  <SrcNodeCode>A1000142000001</SrcNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                </Forward3020>""");

    private static final String INVALID_MISSING_SERIALNO_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "3020",
            "30200000000000000004", "00000000000000000000", "20260514", """
                <RealHead3020>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000004</TransitionNo>
                </RealHead3020>
                <Forward3020>
                  <SrcNodeCode>A1000142000001</SrcNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                </Forward3020>""");

    @Test
    void valid3020_resultNull_addWordNull_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3020,
                VALID_RESULT_NULL_ADDWORD_NULL_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3020 Result+AddWord both omitted errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3020_resultFilled_addWordNull_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3020,
                VALID_RESULT_FILLED_ADDWORD_NULL_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3020 Result filled, AddWord omitted errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3020_resultNull_addWordFilled_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3020,
                VALID_RESULT_NULL_ADDWORD_FILLED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3020 Result omitted, AddWord filled errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3020_missingSerialNo_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3020,
                INVALID_MISSING_SERIALNO_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing required body SerialNo field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("SerialNo"));
    }

    @Test
    void registrySupportsMsg3020() {
        assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_3020)).isNotNull();
    }
}
