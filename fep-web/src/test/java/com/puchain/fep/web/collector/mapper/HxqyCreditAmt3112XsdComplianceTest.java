package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.HxqyCreditAmt3112FieldMapper;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Plan B T3: 3112 HxqyCreditAmt mapper 真 XsdValidator on SUT 集成测试
 * （BatchHead3112 / RequestHead；hxqyInfo nested list 1 条，复用 HxqyInfo）。
 *
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class HxqyCreditAmt3112XsdComplianceTest {

    @Autowired
    private HxqyCreditAmt3112FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> hxqy = new HashMap<>();
        hxqy.put("hxqy_name", "核心企业甲");
        hxqy.put("hxqy_code", "91110000111111111X"); // 18 chars

        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "SN3112000000000000000000000001"); // 30 chars
        raw.put("query_date", "20261128");
        raw.put("hxqy_info_num", "1");
        raw.put("hxqy_info", List.of(hxqy));

        HxqyCreditAmt3112 body = (HxqyCreditAmt3112) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3112", body))
                .doesNotThrowAnyException();
    }
}
