package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.PzCheckQuery3107FieldMapper;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Plan B T2: 3107 PzCheckQuery mapper 真 XsdValidator on SUT 集成测试
 * （BatchHead3107 / RequestHead；hxqyInfo nested list 1 条）。
 *
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class PzCheckQuery3107XsdComplianceTest {

    @Autowired
    private PzCheckQuery3107FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> hxqy = new HashMap<>();
        hxqy.put("hxqy_name", "核心企业甲");
        hxqy.put("hxqy_code", "91110000111111111X"); // 18 chars

        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN3107000000000000000000000001"); // 30 chars
        raw.put("check_date", "20261128");
        raw.put("hxqy_num", "1");
        raw.put("hxqy_info", List.of(hxqy));

        PzCheckQuery3107 body = (PzCheckQuery3107) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3107", body))
                .doesNotThrowAnyException();
    }
}
