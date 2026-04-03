package com.puchain.fep.transport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 TransportAutoConfiguration 能被 Spring 上下文正确加载。
 */
@SpringBootTest(classes = TransportAutoConfiguration.class)
class TransportAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void transportAutoConfigurationBeanExists() {
        assertNotNull(context.getBean(TransportAutoConfiguration.class));
    }
}
