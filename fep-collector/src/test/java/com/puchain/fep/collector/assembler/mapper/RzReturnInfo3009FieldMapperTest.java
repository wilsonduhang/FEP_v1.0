package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RzReturnInfo3009FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private RzReturnInfo3009FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new RzReturnInfo3009FieldMapper(props);
    }

    @Test
    void happyPath_allRequired_shouldFillBody() {
        Map<String, Object> raw = baseRequired();
        raw.put("rz_phase_info", "审批通过");

        RzReturnInfo3009 body = (RzReturnInfo3009) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank();
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getPlatApplyNo()).isEqualTo("PLAT202611280001");
        assertThat(body.getHxqyName()).isEqualTo("核心企业 A");
        assertThat(body.getRzpzNo()).isEqualTo("PZ202611280001");
        assertThat(body.getRzPhaseCode()).isEqualTo("99");
        assertThat(body.getRzPhaseInfo()).isEqualTo("审批通过");
        assertThat(body.getRzAmtInfo()).isNull();
        assertThat(body.getDbInfo()).isNull();
        assertThat(body.getExtInfo()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "plat_apply_no, platApplyNo",
            "hxqy_name, hxqyName",
            "rzpz_no, rzpzNo",
            "rz_phase_code, rzPhaseCode"
    })
    void missingRequired_shouldThrow(String rawKey, String logicalField) {
        Map<String, Object> raw = baseRequired();
        raw.remove(rawKey);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3009")
                .hasMessageContaining(logicalField);
    }

    @Test
    void optionalRzPhaseInfoMissing_shouldSkip() {
        RzReturnInfo3009 body = (RzReturnInfo3009) mapper.toMessageBody(baseRequired());
        assertThat(body.getRzPhaseInfo()).isNull();
    }

    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        RzReturnInfo3009 body = (RzReturnInfo3009) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3009: sendNodeCode");
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
        raw.put("serial_no", "FIXSERIAL000000000000000000005");
        raw.put("plat_apply_no", "PLAT202611280001");
        raw.put("hxqy_name", "核心企业 A");
        raw.put("rzpz_no", "PZ202611280001");
        raw.put("rz_phase_code", "99");
        return raw;
    }
}
