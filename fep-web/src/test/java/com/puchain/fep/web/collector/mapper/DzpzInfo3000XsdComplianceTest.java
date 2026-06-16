package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.DzpzInfo3000FieldMapper;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 3000 DzpzInfo mapper 真 XsdValidator on SUT 集成测试（B4）。
 *
 * <p>验证 {@link DzpzInfo3000FieldMapper#toMessageBody(Map)} 产出经 JAXB marshal +
 * 完整 CFX envelope 包裹后通过真 {@code XsdValidator}（红线
 * feedback_xsd_compliance_fix_real_validator_on_sut +
 * feedback_xsd_validator_requires_full_envelope_redline）。
 *
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class DzpzInfo3000XsdComplianceTest {

    @Autowired
    private DzpzInfo3000FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3000SERIAL00000000000000001");  // 30 chars
        raw.put("apply_mode", "1");                               // XSD + 母本双合法

        DzpzInfo3000 body = (DzpzInfo3000) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3000", body))
                .doesNotThrowAnyException();
    }
}
