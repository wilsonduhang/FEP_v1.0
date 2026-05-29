package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.ArchiveInfo3102FieldMapper;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Task A7 (Plan A v0.2): 3102 ArchiveInfo mapper 真 XsdValidator on SUT 集成测试。
 *
 * <p>验证 {@link ArchiveInfo3102FieldMapper#toMessageBody(Map)} 产出经 JAXB marshal 后通过
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
class ArchiveInfo3102XsdComplianceTest {

    @Autowired
    private ArchiveInfo3102FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3102SERIAL00000000000000002");  // 30 chars
        raw.put("apply_mode", "1");
        raw.put("hxqy_name", "核心企业甲");
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("rzqy_name", "融资企业乙");
        raw.put("rzqy_code", "91110000222222222Y");

        ArchiveInfo3102 body = (ArchiveInfo3102) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3102", body))
                .doesNotThrowAnyException();
    }
}
