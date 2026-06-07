package com.puchain.fep.processor;

import com.puchain.fep.processor.validation.rule.RuleDefinitionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FEP Processor 模块自动配置。
 *
 * <p>启用 {@link RuleDefinitionProperties} 绑定，使配置驱动的业务校验规则
 * （{@code fep.validation.rules}）可被 {@code ConfiguredRuleFactory} 在启动期装配。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(RuleDefinitionProperties.class)
public class ProcessorAutoConfiguration {
}
