package com.puchain.fep.processor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 ProcessorAutoConfiguration 能被 Spring 上下文正确加载。
 */
@SpringBootTest(classes = ProcessorAutoConfiguration.class)
class ProcessorAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void processorAutoConfigurationBeanExists() {
        assertNotNull(context.getBean(ProcessorAutoConfiguration.class));
    }
}
