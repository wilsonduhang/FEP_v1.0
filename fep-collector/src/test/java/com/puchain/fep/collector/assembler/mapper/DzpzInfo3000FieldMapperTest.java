package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DzpzInfo3000FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private DzpzInfo3000FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new DzpzInfo3000FieldMapper(props);
    }

    @Test
    void happyPath_allRequired_shouldFillBody() {
        DzpzInfo3000 body = (DzpzInfo3000) mapper.toMessageBody(baseRequired());

        assertThat(body.getSerialNo()).isNotBlank().hasSize(30);
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getApplyMode()).isEqualTo("1");   // 母本 {1,2} 合法
        // 嵌套可选块本 Plan 不映射 → null
        assertThat(body.getPzInfo()).isNull();
        assertThat(body.getExtInfo()).isNull();
    }

    @Test
    void missingApplyMode_shouldThrow() {
        Map<String, Object> raw = baseRequired();
        raw.remove("apply_mode");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3000")
                .hasMessageContaining("applyMode");
    }

    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        DzpzInfo3000 body = (DzpzInfo3000) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3000: sendNodeCode");
    }

    @Test
    void institutionCodeInvalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    @ParameterizedTest
    @CsvSource({"1", "2"})
    void applyMode_acceptsMubonLegalValues(String legal) {
        Map<String, Object> raw = baseRequired();
        raw.put("apply_mode", legal);
        DzpzInfo3000 body = (DzpzInfo3000) mapper.toMessageBody(raw);
        assertThat(body.getApplyMode()).isEqualTo(legal);
    }

    private static Map<String, Object> baseRequired() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3000SERIAL00000000000000001");  // 30 chars
        raw.put("apply_mode", "1");                               // 母本 {1,2}
        return raw;
    }
}
