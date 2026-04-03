package com.puchain.fep.converter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 ConverterAutoConfiguration 能被 Spring 上下文正确加载。
 */
@SpringBootTest(classes = ConverterAutoConfiguration.class)
class ConverterAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void converterAutoConfigurationBeanExists() {
        assertNotNull(context.getBean(ConverterAutoConfiguration.class));
    }
}
