package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RzApplyInfo3105FieldMapper} 单元测试（Plan B T1）。
 *
 * <p>3105 融资申请：12 必填标量 + 4 可选标量 + StdBizMode default="11" 兜底；
 * 12 嵌套类型全 minOccurs=0 留 stub（本 Plan 不映射）。
 */
class RzApplyInfo3105FieldMapperTest {

    private static final String INSTITUTION_CODE = "12345678901234";

    private RzApplyInfo3105FieldMapper mapper;

    @BeforeEach
    void setUp() {
        final CollectorProperties props = new CollectorProperties();
        props.setInstitutionCode(INSTITUTION_CODE);
        mapper = new RzApplyInfo3105FieldMapper(props);
    }

    private Map<String, Object> requiredRaw() {
        final Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN3105000000000000000000000001"); // 30 chars
        raw.put("apply_mode", "1");
        raw.put("plat_apply_no", "PLAT202611280001");
        raw.put("hxqy_name", "核心企业甲");
        raw.put("hxqy_code", "91110000111111111X"); // 18 chars
        raw.put("rzpz_no", "PZ202611280001");
        raw.put("rzqy_name", "融资企业乙");
        raw.put("rzqy_code", "91110000222222222Y"); // 18 chars
        raw.put("rzqy_plat_no", "PLATRZQY0001"); // 12 chars, minLen=10
        return raw;
    }

    @Test
    void toMessageBody_allRequiredScalars_populatesBody() {
        final RzApplyInfo3105 body = (RzApplyInfo3105) mapper.toMessageBody(requiredRaw());

        assertThat(body.getSerialNo()).isEqualTo("SN3105000000000000000000000001");
        assertThat(body.getSendNodeCode()).isEqualTo(INSTITUTION_CODE);
        assertThat(body.getDesNodeCode())
                .isEqualTo(RzApplyInfo3105FieldMapper.DES_NODE_CODE_HNDEMP_CENTER);
        assertThat(body.getApplyMode()).isEqualTo("1");
        assertThat(body.getPlatApplyNo()).isEqualTo("PLAT202611280001");
        assertThat(body.getHxqyName()).isEqualTo("核心企业甲");
        assertThat(body.getHxqyCode()).isEqualTo("91110000111111111X");
        assertThat(body.getRzpzNo()).isEqualTo("PZ202611280001");
        assertThat(body.getRzqyName()).isEqualTo("融资企业乙");
        assertThat(body.getRzqyCode()).isEqualTo("91110000222222222Y");
        assertThat(body.getRzqyPlatNo()).isEqualTo("PLATRZQY0001");
    }

    @Test
    void toMessageBody_stdBizModeMissing_defaultsTo11() {
        final RzApplyInfo3105 body = (RzApplyInfo3105) mapper.toMessageBody(requiredRaw());
        assertThat(body.getStdBizMode())
                .as("StdBizMode XSD default=11 when raw absent")
                .isEqualTo("11");
    }

    @Test
    void toMessageBody_stdBizModeProvided_usesProvided() {
        final Map<String, Object> raw = requiredRaw();
        raw.put("std_biz_mode", "13");
        final RzApplyInfo3105 body = (RzApplyInfo3105) mapper.toMessageBody(raw);
        assertThat(body.getStdBizMode()).isEqualTo("13");
    }

    @Test
    void toMessageBody_optionalScalarsPresent_populated() {
        final Map<String, Object> raw = requiredRaw();
        raw.put("branch_bank_code", "BR001");
        raw.put("dbqy_name", "担保企业丙");
        raw.put("dbqy_code", "91110000333333333Z"); // 18 chars
        raw.put("rzqy_addr", "湖南省长沙市");

        final RzApplyInfo3105 body = (RzApplyInfo3105) mapper.toMessageBody(raw);
        assertThat(body.getBranchBankCode()).isEqualTo("BR001");
        assertThat(body.getDbqyName()).isEqualTo("担保企业丙");
        assertThat(body.getDbqyCode()).isEqualTo("91110000333333333Z");
        assertThat(body.getRzqyAddr()).isEqualTo("湖南省长沙市");
    }

    @Test
    void toMessageBody_optionalScalarsAbsent_skipped() {
        final RzApplyInfo3105 body = (RzApplyInfo3105) mapper.toMessageBody(requiredRaw());
        assertThat(body.getBranchBankCode()).isNull();
        assertThat(body.getDbqyName()).isNull();
        assertThat(body.getDbqyCode()).isNull();
        assertThat(body.getRzqyAddr()).isNull();
    }

    @Test
    void toMessageBody_missingApplyMode_throws() {
        final Map<String, Object> raw = requiredRaw();
        raw.remove("apply_mode");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3105: applyMode")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
    }
}
