package com.puchain.fep.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 CommonAutoConfiguration 能被 Spring 上下文正确加载。
 */
@SpringBootTest(classes = CommonAutoConfiguration.class)
class CommonAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void commonAutoConfigurationBeanExists() {
        assertNotNull(context.getBean(CommonAutoConfiguration.class));
    }
}
