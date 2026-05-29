package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task A1: ArchiveInfo3102FieldMapper dedicated unit test
 * (Plan 2026-05-28-collector-mapper-mode3-boil-lake §A1 safety net for Task A2 refactor).
 *
 * <p>测试覆盖：happy path / 5 必填缺失 / 4 可选 / institutionCode 2 异常
 *
 * @since 1.0.0
 */
class ArchiveInfo3102FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private ArchiveInfo3102FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new ArchiveInfo3102FieldMapper(props);
    }

    @Test
    void happyPath_allFieldsPresent_shouldFillBody() {
        Map<String, Object> raw = baseRequired();
        raw.put("group_name", "集团 A");
        raw.put("group_code", "91110000444444444A");
        raw.put("rzqy_plat_no", "PLAT202611280001");
        raw.put("rzqy_ca_filename", "ca.pdf");

        ArchiveInfo3102 body = (ArchiveInfo3102) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank();
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getApplyMode()).isEqualTo("01");
        assertThat(body.getHxqyName()).isEqualTo("核心企业 A");
        assertThat(body.getHxqyCode()).isEqualTo("91110000111111111X");
        assertThat(body.getRzqyName()).isEqualTo("融资企业 A");
        assertThat(body.getRzqyCode()).isEqualTo("91110000222222222Y");
        assertThat(body.getGroupName()).isEqualTo("集团 A");
        assertThat(body.getGroupCode()).isEqualTo("91110000444444444A");
        assertThat(body.getRzqyPlatNo()).isEqualTo("PLAT202611280001");
        assertThat(body.getRzqyCAFilename()).isEqualTo("ca.pdf");
    }

    @ParameterizedTest
    @CsvSource({
            "apply_mode, applyMode",
            "hxqy_name, hxqyName",
            "hxqy_code, hxqyCode",
            "rzqy_name, rzqyName",
            "rzqy_code, rzqyCode"
    })
    void missingRequired_shouldThrowWithFieldName(String rawKey, String logicalField) {
        Map<String, Object> raw = baseRequired();
        raw.remove(rawKey);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3102")
                .hasMessageContaining(logicalField);
    }

    @Test
    void optionalFieldsMissing_shouldSkipSetter() {
        ArchiveInfo3102 body = (ArchiveInfo3102) mapper.toMessageBody(baseRequired());

        assertThat(body.getGroupName()).isNull();
        assertThat(body.getGroupCode()).isNull();
        assertThat(body.getRzqyPlatNo()).isNull();
        assertThat(body.getRzqyCAFilename()).isNull();
    }

    /**
     * serial_no 缺失 → uuid32 兜底，XSD 合规长度 30。
     * NOTE: RED until Task A2 adds serialNoOrFallback() — documents latent bug (uuid32=32 chars).
     */
    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        ArchiveInfo3102 body = (ArchiveInfo3102) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institution-code 未配置");
    }

    @Test
    void institutionCodeInvalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    private static Map<String, Object> baseRequired() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIXSERIAL000000000000000000002");
        raw.put("apply_mode", "01");
        raw.put("hxqy_name", "核心企业 A");
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("rzqy_name", "融资企业 A");
        raw.put("rzqy_code", "91110000222222222Y");
        return raw;
    }
}
