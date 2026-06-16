package com.puchain.fep.converter;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.MessageSignPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * 验证 ConverterAutoConfiguration 能被 Spring 上下文正确加载。
 *
 * <p>由于 {@code MessageSigner}/{@code MessageVerifier} 依赖 {@link MessageSignPort}（GM S2b
 * 形态 C-ev）、{@code MessageEncryptor} 依赖 {@link CryptoService}（实现类均位于
 * {@code fep-security-impl} 的密钥材料隔离域——2026-06-07 解禁后分层隔离保留），测试通过 {@link TestSignServiceConfig}
 * 提供 Mockito mock 避免测试期依赖真实 SM2/SM4 实现。</p>
 */
@SpringBootTest(classes = {ConverterAutoConfiguration.class, ConverterAutoConfigurationTest.TestSignServiceConfig.class})
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

    /**
     * 为测试上下文提供 {@link MessageSignPort} 与 {@link CryptoService} Mock bean。
     *
     * <p>生产环境由 {@code fep-security-impl}（impl）或 {@code fep-security-mock}（mock）按
     * provider 注入真实/mock 实现；本 Mock 仅用于验证 converter 自动配置的 bean 拓扑装配，
     * 不执行任何加密运算。</p>
     */
    @Configuration
    static class TestSignServiceConfig {

        @Bean
        MessageSignPort messageSignPort() {
            return mock(MessageSignPort.class);
        }

        @Bean
        CryptoService cryptoService() {
            return mock(CryptoService.class);
        }
    }
}
