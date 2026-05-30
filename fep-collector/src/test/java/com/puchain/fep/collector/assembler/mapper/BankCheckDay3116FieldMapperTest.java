package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.CheckDetailInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankCheckDay3116FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private BankCheckDay3116FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new BankCheckDay3116FieldMapper(props);
    }

    @Test
    void happyPath_oneDetail_shouldFillBody() {
        Map<String, Object> raw = baseRequiredTop();
        raw.put("check_detail_info", List.of(baseRequiredDetail()));

        BankCheckDay3116 body = (BankCheckDay3116) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank();
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getHxqyName()).isEqualTo("核心企业 A");
        assertThat(body.getHxqyCode()).isEqualTo("91110000111111111X");
        assertThat(body.getCheckDate()).isEqualTo("20261128");
        assertThat(body.getCheckDetailNum()).isEqualTo("1");
        assertThat(body.getCheckDetailInfo()).hasSize(1);

        CheckDetailInfo detail = body.getCheckDetailInfo().get(0);
        assertThat(detail.getSid()).isEqualTo("1");
        assertThat(detail.getPlatNodeCode()).isEqualTo("A1000143000888");
        assertThat(detail.getBizType()).isEqualTo("01");
        assertThat(detail.getRzqyName()).isEqualTo("融资企业 A");
        assertThat(detail.getRzqyCode()).isEqualTo("91110000222222222Y");
        assertThat(detail.getRzAmt()).isEqualTo("100000.00");
        assertThat(detail.getRzRate()).isEqualTo("0.0480");
        assertThat(detail.getRzStartDate()).isEqualTo("20261101");
        assertThat(detail.getRzEndDate()).isEqualTo("20261130");
        assertThat(detail.getAmt()).isEqualTo("100000.00");
        assertThat(detail.getPzNo()).isNull();
        assertThat(detail.getBillNo()).isNull();
        assertThat(detail.getRepayStyle()).isNull();
        assertThat(body.getExtInfo()).isNull();
    }

    @Test
    void missingHxqyName_top_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        raw.remove("hxqy_name");
        raw.put("check_detail_info", List.of(baseRequiredDetail()));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116: hxqyName");
    }

    @Test
    void missingCheckDetailInfoList_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116: checkDetailInfo");
    }

    @Test
    void emptyCheckDetailInfoList_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        raw.put("check_detail_info", new ArrayList<>());
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116: checkDetailInfo")
                .hasMessageContaining("non-empty");
    }

    @Test
    void detailMissingRzAmt_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        Map<String, Object> detail = baseRequiredDetail();
        detail.remove("rz_amt");
        raw.put("check_detail_info", List.of(detail));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116")
                .hasMessageContaining("rzAmt");
    }

    @Test
    void detailMissingAmt_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        Map<String, Object> detail = baseRequiredDetail();
        detail.remove("amt");
        raw.put("check_detail_info", List.of(detail));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116")
                .hasMessageContaining("amt");
    }

    @Test
    void listSizeExceeds200_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        List<Map<String, Object>> big = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            big.add(baseRequiredDetail());
        }
        raw.put("check_detail_info", big);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("checkDetailInfo")
                .hasMessageContaining("max 200");
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        Map<String, Object> raw = baseRequiredTop();
        raw.put("check_detail_info", List.of(baseRequiredDetail()));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116: sendNodeCode");
    }

    private static Map<String, Object> baseRequiredTop() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIXSERIAL000000000000000000004");
        raw.put("hxqy_name", "核心企业 A");
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("check_date", "20261128");
        raw.put("check_detail_num", "1");
        return raw;
    }

    private static Map<String, Object> baseRequiredDetail() {
        Map<String, Object> d = new HashMap<>();
        d.put("sid", "1");
        d.put("plat_node_code", "A1000143000888");
        d.put("biz_type", "01");
        d.put("rzqy_name", "融资企业 A");
        d.put("rzqy_code", "91110000222222222Y");
        d.put("rz_amt", "100000.00");
        d.put("rz_rate", "0.0480");
        d.put("rz_start_date", "20261101");
        d.put("rz_end_date", "20261130");
        d.put("amt", "100000.00");
        return d;
    }
}
