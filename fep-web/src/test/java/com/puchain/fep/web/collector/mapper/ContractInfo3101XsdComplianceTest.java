package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.ContractInfo3101FieldMapper;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Task A7 (Plan A v0.2): 3101 ContractInfo mapper 真 XsdValidator on SUT 集成测试。
 *
 * <p>验证 {@link ContractInfo3101FieldMapper#toMessageBody(Map)} 产出经 JAXB marshal 后通过
 * 真 {@code XsdValidator} 校验（红线 feedback_xsd_compliance_fix_real_validator_on_sut）。
 *
 * <p>Pattern 沿用 {@code OutboundEnvelopeXsdComplianceTest}（R-NEW-1, 2026-05-27）。
 * fixture 字段值满足 DataType.xsd facet 约束（SerialNo length=30 / NodeCode 14 位 / USCI 18 位）。
 *
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class ContractInfo3101XsdComplianceTest {

    @Autowired
    private ContractInfo3101FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3101SERIAL00000000000000001");  // 30 chars
        raw.put("contract_no", "CON202611280001");
        raw.put("contract_type", "01");
        raw.put("digital_seal", "1");
        raw.put("contract_filename", "contract.pdf");
        raw.put("jfqy_name", "甲方公司");
        raw.put("yfqy_name", "乙方公司");
        // optional fields
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("cert_filename", "cert.pdf");
        raw.put("jfqy_code", "91110000222222222Y");
        raw.put("yfqy_code", "91110000333333333Z");
        raw.put("sx_date", "20261128");
        raw.put("qz_date", "20271128");

        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3101", body))
                .doesNotThrowAnyException();
    }
}
