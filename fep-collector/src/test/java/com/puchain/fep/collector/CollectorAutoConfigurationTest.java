package com.puchain.fep.collector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 CollectorAutoConfiguration 能被 Spring 上下文正确加载，
 * 且 {@link CollectorProperties} 可绑定 fep.collector.* 配置。
 */
@SpringBootTest(classes = CollectorAutoConfiguration.class)
@TestPropertySource(properties = {
        "fep.collector.institution-code=FEP_TEST_001",
        "fep.collector.batch-size=200",
        "fep.collector.lock-ttl-millis=120000",
        "fep.collector.retry.max-attempts=4",
        "fep.collector.retry.backoff-millis=1500"
})
class CollectorAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CollectorProperties properties;

    @Test
    void contextLoadsWithCollectorPropertiesBeanRegistered() {
        assertThat(context.containsBeanDefinition(
                "fep.collector-com.puchain.fep.collector.CollectorProperties"))
                .as("Spring 上下文必须含 CollectorProperties bean 定义（@EnableConfigurationProperties 注册）")
                .isTrue();
        assertThat(context.getBean(CollectorProperties.class))
                .as("CollectorProperties bean 必须可由类型解析")
                .isSameAs(properties);
    }

    @Test
    void collectorAutoConfigurationBeanExposesEnableConfigurationProperties() {
        assertThat(CollectorAutoConfiguration.class.isAnnotationPresent(
                org.springframework.boot.context.properties.EnableConfigurationProperties.class))
                .as("CollectorAutoConfiguration 必须标注 @EnableConfigurationProperties")
                .isTrue();
        assertThat(context.getBean(CollectorAutoConfiguration.class))
                .as("CollectorAutoConfiguration bean 必须在上下文中可解析（不使用 getClass 避免 CGLIB 代理混淆）")
                .isNotNull();
    }

    @Test
    void collectorPropertiesBeanShouldBeRegisteredAndBound() {
        assertThat(properties).isNotNull();
        assertThat(properties.getInstitutionCode()).isEqualTo("FEP_TEST_001");
        assertThat(properties.getBatchSize()).isEqualTo(200);
        assertThat(properties.getLockTtlMillis()).isEqualTo(120_000L);
        assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(4);
        assertThat(properties.getRetry().getBackoffMillis()).isEqualTo(1_500L);
    }
}
