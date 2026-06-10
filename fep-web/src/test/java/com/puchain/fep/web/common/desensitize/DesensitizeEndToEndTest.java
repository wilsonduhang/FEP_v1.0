package com.puchain.fep.web.common.desensitize;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端：经 Spring 注入的 ObjectMapper（SpringHandlerInstantiator + DesensitizeService）
 * 序列化带 @Desensitize 的 DTO，证明 HandlerInstantiator 注入链路真机生效（非单测 addSerializer 假绿）。
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
class DesensitizeEndToEndTest {

    record Dto(@Desensitize(DesensitizeType.PHONE) String phone) {
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void springInjectedObjectMapper_desensitizesAnnotatedField() throws Exception {
        final String json = objectMapper.writeValueAsString(new Dto("13800138000"));
        assertThat(json).contains("\"phone\":\"138****8000\"");
    }
}
