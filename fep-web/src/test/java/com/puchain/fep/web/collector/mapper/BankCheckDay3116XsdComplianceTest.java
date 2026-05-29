package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.BankCheckDay3116FieldMapper;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Task A7 (Plan A v0.2): 3116 BankCheckDay mapper 真 XsdValidator on SUT 集成测试。
 *
 * <p>验证 {@link BankCheckDay3116FieldMapper#toMessageBody(Map)} 产出经 JAXB marshal 后通过
 * 真 {@code XsdValidator} 校验（红线 feedback_xsd_compliance_fix_real_validator_on_sut）。
 *
 * <p>CheckDetailInfo nested list: 1 条明细，含 10 必填字段（sid/platNodeCode/bizType/
 * rzqyName/rzqyCode/rzAmt/rzRate/rzStartDate/rzEndDate/amt）。
 *
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class BankCheckDay3116XsdComplianceTest {

    @Autowired
    private BankCheckDay3116FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> detail = new HashMap<>();
        detail.put("sid", "1");
        detail.put("plat_node_code", "A1000143000888");
        detail.put("biz_type", "01");
        detail.put("rzqy_name", "融资企业丙");
        detail.put("rzqy_code", "91110000333333333Z");
        detail.put("rz_amt", "100000.00");
        detail.put("rz_rate", "0.0480");
        detail.put("rz_start_date", "20261101");
        detail.put("rz_end_date", "20261130");
        detail.put("amt", "100000.00");

        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3116SERIAL00000000000000004");  // 30 chars
        raw.put("hxqy_name", "核心企业甲");
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("check_date", "20261128");
        raw.put("check_detail_num", "1");
        raw.put("check_detail_info", List.of(detail));

        BankCheckDay3116 body = (BankCheckDay3116) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3116", body))
                .doesNotThrowAnyException();
    }
}
