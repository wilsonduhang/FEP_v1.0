package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
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
 * {@link HxqyCreditAmt3112FieldMapper} 单元测试（Plan B T3）。
 *
 * <p>3112 核心企业授信查询请求：5 必填标量 + required hxqyInfo nested list（1..200，复用 HxqyInfo）。
 */
class HxqyCreditAmt3112FieldMapperTest {

    private static final String INSTITUTION_CODE = "12345678901234";

    private HxqyCreditAmt3112FieldMapper mapper;

    @BeforeEach
    void setUp() {
        final CollectorProperties props = new CollectorProperties();
        props.setInstitutionCode(INSTITUTION_CODE);
        mapper = new HxqyCreditAmt3112FieldMapper(props);
    }

    private Map<String, Object> hxqy(final String name, final String code) {
        final Map<String, Object> m = new HashMap<>();
        m.put("hxqy_name", name);
        m.put("hxqy_code", code);
        return m;
    }

    private Map<String, Object> requiredRaw() {
        final Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN3112000000000000000000000001"); // 30 chars
        raw.put("query_date", "20261128");
        raw.put("hxqy_info_num", "1");
        raw.put("hxqy_info", List.of(hxqy("核心企业甲", "91110000111111111X")));
        return raw;
    }

    @Test
    void toMessageBody_happyPath_populatesScalarsAndList() {
        final HxqyCreditAmt3112 body = (HxqyCreditAmt3112) mapper.toMessageBody(requiredRaw());

        assertThat(body.getSerialNo()).isEqualTo("SN3112000000000000000000000001");
        assertThat(body.getSendNodeCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(body.getDesNodeCode())
                .isEqualTo(HxqyCreditAmt3112FieldMapper.DES_NODE_CODE_HNDEMP_CENTER);
        assertThat(body.getQueryDate()).isEqualTo("20261128");
        assertThat(body.getHxqyInfoNum()).isEqualTo("1");
        assertThat(body.getHxqyInfo()).hasSize(1);
        assertThat(body.getHxqyInfo().get(0).getHxqyCode()).isEqualTo("91110000111111111X");
    }

    @Test
    void toMessageBody_missingHxqyInfo_throws() {
        final Map<String, Object> raw = requiredRaw();
        raw.remove("hxqy_info");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3112: hxqyInfo")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
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
    void toMessageBody_missingQueryDate_throws() {
        final Map<String, Object> raw = requiredRaw();
        raw.remove("query_date");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3112: queryDate");
    }
}
