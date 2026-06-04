package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PzCheckQuery3107FieldMapper} 单元测试（Plan B T2）。
 *
 * <p>3107 平台凭证对账：5 必填标量 + required hxqyInfo nested list（1..200）。
 */
class PzCheckQuery3107FieldMapperTest {

    private static final String INSTITUTION_CODE = "12345678901234";

    private PzCheckQuery3107FieldMapper mapper;

    @BeforeEach
    void setUp() {
        final CollectorProperties props = new CollectorProperties();
        props.setInstitutionCode(INSTITUTION_CODE);
        mapper = new PzCheckQuery3107FieldMapper(props);
    }

    private Map<String, Object> hxqy(final String name, final String code) {
        final Map<String, Object> m = new HashMap<>();
        m.put("hxqy_name", name);
        m.put("hxqy_code", code);
        return m;
    }

    private Map<String, Object> requiredRaw() {
        final Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN3107000000000000000000000001"); // 30 chars
        raw.put("check_date", "20261128");
        raw.put("hxqy_num", "1");
        raw.put("hxqy_info", List.of(hxqy("核心企业甲", "91110000111111111X")));
        return raw;
    }

    @Test
    void toMessageBody_happyPath_populatesScalarsAndList() {
        final PzCheckQuery3107 body = (PzCheckQuery3107) mapper.toMessageBody(requiredRaw());

        assertThat(body.getSerialNo()).isEqualTo("SN3107000000000000000000000001");
        assertThat(body.getSendNodeCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(body.getDesNodeCode())
                .isEqualTo(PzCheckQuery3107FieldMapper.DES_NODE_CODE_HNDEMP_CENTER);
        assertThat(body.getCheckDate()).isEqualTo("20261128");
        assertThat(body.getHxqyNum()).isEqualTo("1");
        assertThat(body.getHxqyInfo()).hasSize(1);
        assertThat(body.getHxqyInfo().get(0).getHxqyName()).isEqualTo("核心企业甲");
        assertThat(body.getHxqyInfo().get(0).getHxqyCode()).isEqualTo("91110000111111111X");
    }

    @Test
    void toMessageBody_missingHxqyInfo_throws() {
        final Map<String, Object> raw = requiredRaw();
        raw.remove("hxqy_info");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3107: hxqyInfo")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
    }

    @Test
    void toMessageBody_emptyHxqyInfo_throws() {
        final Map<String, Object> raw = requiredRaw();
        raw.put("hxqy_info", List.of());
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3107: hxqyInfo");
    }

    @Test
    void toMessageBody_hxqyInfoExceeds200_throws() {
        final Map<String, Object> raw = requiredRaw();
        final List<Map<String, Object>> list = new ArrayList<>();
        IntStream.range(0, 201).forEach(i -> list.add(hxqy("企业" + i, "91110000111111111X")));
        raw.put("hxqy_info", list);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("exceeds max 200");
    }

    @Test
    void toMessageBody_hxqyEntryMissingName_throws() {
        final Map<String, Object> raw = requiredRaw();
        final Map<String, Object> entry = hxqy("核心企业甲", "91110000111111111X");
        entry.remove("hxqy_name");
        raw.put("hxqy_info", List.of(entry));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3107: hxqyName")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
    }

    @Test
    void toMessageBody_hxqyEntryMissingCode_throws() {
        final Map<String, Object> raw = requiredRaw();
        final Map<String, Object> entry = hxqy("核心企业甲", "91110000111111111X");
        entry.remove("hxqy_code");
        raw.put("hxqy_info", List.of(entry));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3107: hxqyCode");
    }

    @Test
    void toMessageBody_missingCheckDate_throws() {
        final Map<String, Object> raw = requiredRaw();
        raw.remove("check_date");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3107: checkDate");
    }
}
