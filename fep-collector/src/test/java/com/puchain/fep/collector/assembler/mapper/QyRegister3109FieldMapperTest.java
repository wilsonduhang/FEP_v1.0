package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task A3: QyRegister3109FieldMapper unit test
 * (Plan 2026-05-28-collector-mapper-mode3-boil-lake §A3).
 */
class QyRegister3109FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private QyRegister3109FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new QyRegister3109FieldMapper(props);
    }

    @Test
    void happyPath_allRequired_shouldFillBody() {
        Map<String, Object> raw = baseRequired();
        QyRegister3109 body = (QyRegister3109) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank().hasSize(30);
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getQyFlag()).isEqualTo("1");
        assertThat(body.getHxqyInfo()).isNull();
        assertThat(body.getQyAccLockInfo()).isNull();
        assertThat(body.getPlatInfo()).isNull();
        assertThat(body.getExtInfo()).isNull();
    }

    @Test
    void missingQyFlag_shouldThrow() {
        Map<String, Object> raw = baseRequired();
        raw.remove("qy_flag");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3109: qyFlag");
    }

    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        QyRegister3109 body = (QyRegister3109) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3109: sendNodeCode");
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
        raw.put("serial_no", "FIXSERIAL000000000000000000003");
        raw.put("qy_flag", "1");
        return raw;
    }
}
