package com.puchain.fep.web;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.SignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * FEP 应用冒烟测试 — 验证全模块 Spring 上下文启动成功。
 */
@SpringBootTest
class FepApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context, "Spring ApplicationContext should load successfully");
    }

    @Test
    void cryptoServiceBeanExists() {
        assertNotNull(context.getBean(CryptoService.class),
                "CryptoService bean should be registered (mock in dev profile)");
    }

    @Test
    void signServiceBeanExists() {
        assertNotNull(context.getBean(SignService.class),
                "SignService bean should be registered (mock in dev profile)");
    }
}
