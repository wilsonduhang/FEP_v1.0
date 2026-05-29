package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task A1: ContractInfo3101FieldMapper dedicated unit test
 * (Plan 2026-05-28-collector-mapper-mode3-boil-lake §A1 safety net for Task A2 refactor).
 *
 * <p>测试覆盖：happy path / 6 必填缺失 / 6 可选 / 4+4 boolean 字面 / 1 非法 boolean / institutionCode 2 异常
 *
 * @since 1.0.0
 */
class ContractInfo3101FieldMapperTest {

    /** 14 位 NodeCode 合法测试值（满足 XSD NodeCode facet）。 */
    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private ContractInfo3101FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new ContractInfo3101FieldMapper(props);
    }

    /** 完整 raw → body 所有 setter 命中（含 6 可选）。 */
    @Test
    void happyPath_allFieldsPresent_shouldFillBody() {
        Map<String, Object> raw = baseRequired();
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("cert_filename", "cert.pdf");
        raw.put("jfqy_code", "91110000222222222Y");
        raw.put("yfqy_code", "91110000333333333Z");
        raw.put("sx_date", "20261128");
        raw.put("qz_date", "20271128");

        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank().hasSize(30);
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getContractNo()).isEqualTo("CON202611280001");
        assertThat(body.getContractType()).isEqualTo("01");
        assertThat(body.getDigitalSeal()).isEqualTo("1");
        assertThat(body.getContractFilename()).isEqualTo("contract.pdf");
        assertThat(body.getJfqyName()).isEqualTo("甲方公司");
        assertThat(body.getYfqyName()).isEqualTo("乙方公司");
        assertThat(body.getHxqyCode()).isEqualTo("91110000111111111X");
        assertThat(body.getCertFilename()).isEqualTo("cert.pdf");
        assertThat(body.getJfqyCode()).isEqualTo("91110000222222222Y");
        assertThat(body.getYfqyCode()).isEqualTo("91110000333333333Z");
        assertThat(body.getSxDate()).isEqualTo("20261128");
        assertThat(body.getQzDate()).isEqualTo("20271128");
    }

    /** 6 必填字段单独缺失 → 抛 FepBusinessException(COLLECT_ASSEMBLE_FAILURE)。 */
    @ParameterizedTest
    @CsvSource({
            "contract_no, contractNo",
            "contract_type, contractType",
            "contract_filename, contractFilename",
            "jfqy_name, jfqyName",
            "yfqy_name, yfqyName"
    })
    void missingRequired_shouldThrowWithFieldName(String rawKey, String logicalField) {
        Map<String, Object> raw = baseRequired();
        raw.remove(rawKey);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3101")
                .hasMessageContaining(logicalField);
    }

    /** digitalSeal 缺失单独测（不在 CsvSource 因为它是特殊 Boolean 字段）。 */
    @Test
    void missingDigitalSeal_shouldThrow() {
        Map<String, Object> raw = baseRequired();
        raw.remove("digital_seal");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3101: digitalSeal");
    }

    /** 6 可选字段全缺失 → 不抛，对应 getter 返 null。 */
    @Test
    void optionalFieldsMissing_shouldSkipSetter() {
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(baseRequired());

        assertThat(body.getHxqyCode()).isNull();
        assertThat(body.getCertFilename()).isNull();
        assertThat(body.getJfqyCode()).isNull();
        assertThat(body.getYfqyCode()).isNull();
        assertThat(body.getSxDate()).isNull();
        assertThat(body.getQzDate()).isNull();
    }

    /** digitalSeal 接受 4 个真值字面 → 规整为 "1"。 */
    @ParameterizedTest
    @ValueSource(strings = {"1", "true", "TRUE", "True"})
    void digitalSealTrueLiterals_shouldYield1(String literal) {
        Map<String, Object> raw = baseRequired();
        raw.put("digital_seal", literal);
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);
        assertThat(body.getDigitalSeal()).isEqualTo("1");
    }

    /** digitalSeal 接受 4 个假值字面 → 规整为 "0"。 */
    @ParameterizedTest
    @ValueSource(strings = {"0", "false", "FALSE", "False"})
    void digitalSealFalseLiterals_shouldYield0(String literal) {
        Map<String, Object> raw = baseRequired();
        raw.put("digital_seal", literal);
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);
        assertThat(body.getDigitalSeal()).isEqualTo("0");
    }

    /** digitalSeal Boolean.TRUE/FALSE 类型 → 同样规整。 */
    @Test
    void digitalSealBooleanType_shouldYieldStringEquivalent() {
        Map<String, Object> raw = baseRequired();
        raw.put("digital_seal", Boolean.TRUE);
        assertThat(((ContractInfo3101) mapper.toMessageBody(raw)).getDigitalSeal()).isEqualTo("1");
        raw.put("digital_seal", Boolean.FALSE);
        assertThat(((ContractInfo3101) mapper.toMessageBody(raw)).getDigitalSeal()).isEqualTo("0");
    }

    /** digitalSeal 非法字面 → 抛业务异常。 */
    @Test
    void digitalSealInvalidLiteral_shouldThrow() {
        Map<String, Object> raw = baseRequired();
        raw.put("digital_seal", "yes");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("invalid field value for 3101")
                .hasMessageContaining("digitalSeal");
    }

    /**
     * serial_no 缺失 → uuid32 兜底，body.serialNo 非 null 且 XSD 合规长度 30。
     * NOTE: This test WILL BE RED until Task A2 adds serialNoOrFallback() — this is intentional,
     * documenting the latent bug (uuid32()=32 chars, XSD requires 30).
     */
    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    /** institutionCode 未配置 → 抛业务异常。 */
    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("sendNodeCode")
                .hasMessageContaining("institution-code 未配置");
    }

    /** institutionCode 长度非 14 → 抛业务异常。 */
    @Test
    void institutionCodeInvalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    /** desNodeCode 始终 = HNDEMP_NODE_CODE（不可被 raw 覆盖）。 */
    @Test
    void desNodeCodeIsAlwaysHndempCenter() {
        Map<String, Object> raw = baseRequired();
        raw.put("des_node_code", "OVERRIDE_TRY");
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
    }

    /**
     * 9 必填 + serial_no 完整 fixture（uuid32 兜底由具体测试覆盖）。
     */
    private static Map<String, Object> baseRequired() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIXSERIAL000000000000000000001");
        raw.put("contract_no", "CON202611280001");
        raw.put("contract_type", "01");
        raw.put("digital_seal", "1");
        raw.put("contract_filename", "contract.pdf");
        raw.put("jfqy_name", "甲方公司");
        raw.put("yfqy_name", "乙方公司");
        return raw;
    }
}
