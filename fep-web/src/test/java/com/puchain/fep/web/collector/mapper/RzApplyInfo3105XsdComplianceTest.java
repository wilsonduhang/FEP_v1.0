package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.RzApplyInfo3105FieldMapper;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Plan B T1: 3105 RzApplyInfo mapper 真 XsdValidator on SUT 集成测试。
 *
 * <p>验证 {@link RzApplyInfo3105FieldMapper#toMessageBody(Map)} 产出经 JAXB marshal +
 * 完整 CFX envelope wrap（BatchHead3105 / RequestHead）后通过真 {@code XsdValidator} 校验
 * （红线 feedback_xsd_compliance_fix_real_validator_on_sut +
 * feedback_xsd_validator_requires_full_envelope）。
 *
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class RzApplyInfo3105XsdComplianceTest {

    @Autowired
    private RzApplyInfo3105FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN3105000000000000000000000001"); // 30 chars
        raw.put("apply_mode", "1");
        raw.put("plat_apply_no", "PLAT202611280001");
        raw.put("std_biz_mode", "11");
        raw.put("hxqy_name", "核心企业甲");
        raw.put("hxqy_code", "91110000111111111X"); // 18 chars
        raw.put("rzpz_no", "PZ202611280001");
        raw.put("rzqy_name", "融资企业乙");
        raw.put("rzqy_code", "91110000222222222Y"); // 18 chars
        raw.put("rzqy_plat_no", "PLATRZQY0001"); // 12 chars, minLen=10

        RzApplyInfo3105 body = (RzApplyInfo3105) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3105", body))
                .doesNotThrowAnyException();
    }
}
