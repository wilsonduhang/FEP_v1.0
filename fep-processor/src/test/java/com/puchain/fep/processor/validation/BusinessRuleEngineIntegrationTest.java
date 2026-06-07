package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.ProcessorAutoConfiguration;
import com.puchain.fep.processor.validation.rule.ConfiguredRuleFactory;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端集成测试：验证 {@code fep.validation.rules} 配置经 {@link ConfiguredRuleFactory}
 * 在 Spring 启动期（{@code @PostConstruct}）装配进 {@link MessageRuleRegistry}，
 * 并由 {@link BusinessRuleValidator} 实际执行。
 *
 * <p>使用 {@code ruletest} profile（{@code application-ruletest.yml}）声明一条 3116 的示例
 * ENUM 规则（Currency ∈ {CNY, USD}）。</p>
 *
 * <p>本 IT 聚焦「配置 → 装配 → 引擎执行」链路；完整同步流水线 XSD→业务关→
 * FAILED(PROC_8507)/COMPLETED 的终态行为已由
 * {@code SyncMessageProcessorServiceTest} 单元测试覆盖，此处不重复拉起持久化/JPA 上下文。</p>
 */
@SpringBootTest(classes = {
        ProcessorAutoConfiguration.class,
        MessageRuleRegistry.class,
        ConfiguredRuleFactory.class,
        BusinessRuleValidator.class
})
@ActiveProfiles("ruletest")
class BusinessRuleEngineIntegrationTest {

    @Autowired
    private MessageRuleRegistry registry;

    @Autowired
    private BusinessRuleValidator businessRuleValidator;

    private static byte[] xml(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void configuredRule_shouldBeRegisteredAtStartup() {
        assertThat(registry.rulesFor(MessageType.MSG_3116)).hasSize(1);
    }

    @Test
    void configuredRule_shouldRejectViolatingMessage() {
        ValidationResult r = businessRuleValidator.validate(
                MessageType.MSG_3116, xml("<CFX><Currency>JPY</Currency></CFX>"));
        assertThat(r.valid()).isFalse();
        assertThat(r.errors().get(0)).contains("Currency");
    }

    @Test
    void configuredRule_shouldAcceptCompliantMessage() {
        ValidationResult r = businessRuleValidator.validate(
                MessageType.MSG_3116, xml("<CFX><Currency>CNY</Currency></CFX>"));
        assertThat(r.valid()).isTrue();
    }

    @Test
    void unconfiguredType_shouldPassThrough() {
        ValidationResult r = businessRuleValidator.validate(
                MessageType.MSG_1001, xml("<CFX><Currency>JPY</Currency></CFX>"));
        assertThat(r.valid()).isTrue();
    }
}
