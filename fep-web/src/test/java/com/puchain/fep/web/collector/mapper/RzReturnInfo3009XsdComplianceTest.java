package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.RzReturnInfo3009FieldMapper;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Task A7 (Plan A v0.2): 3009 RzReturnInfo mapper 真 XsdValidator on SUT 集成测试。
 *
 * <p>验证 {@link RzReturnInfo3009FieldMapper#toMessageBody(Map)} 产出经 JAXB marshal 后通过
 * 真 {@code XsdValidator} 校验（红线 feedback_xsd_compliance_fix_real_validator_on_sut）。
 *
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class RzReturnInfo3009XsdComplianceTest {

    @Autowired
    private RzReturnInfo3009FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3009SERIAL00000000000000005");  // 30 chars
        raw.put("plat_apply_no", "PLAT202611280001");
        raw.put("hxqy_name", "核心企业甲");
        raw.put("rzpz_no", "PZ202611280001");
        raw.put("rz_phase_code", "99");

        RzReturnInfo3009 body = (RzReturnInfo3009) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3009", body))
                .doesNotThrowAnyException();
    }
}
