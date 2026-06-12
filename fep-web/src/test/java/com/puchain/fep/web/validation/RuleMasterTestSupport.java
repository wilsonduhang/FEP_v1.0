package com.puchain.fep.web.validation;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.BusinessRuleValidator;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.rule.ConfiguredRuleFactory;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import com.puchain.fep.processor.validation.rule.RuleDefinitionProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 规则母本测试共享支撑：绑定生产 application.yml 的 fep.validation 规则配置，
 * 装配全量注册表后对样本报文执行业务规则校验。
 */
final class RuleMasterTestSupport {

    private RuleMasterTestSupport() {
    }

    /**
     * 绑定生产 application.yml 的 fep.validation 规则配置。
     *
     * @return 绑定后的规则定义
     * @throws IOException 读取 classpath 配置失败
     */
    static RuleDefinitionProperties bindProductionRules() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources =
                loader.load("application", new ClassPathResource("application.yml"));
        StandardEnvironment env = new StandardEnvironment();
        sources.forEach(env.getPropertySources()::addFirst);
        return Binder.get(env).bind("fep.validation", RuleDefinitionProperties.class)
                .orElseGet(RuleDefinitionProperties::new);
    }

    /**
     * 用生产规则配置（全量注册，含 "*" 通配）校验样本报文。
     *
     * @param msgNo       报文号（须在 {@link MessageType} 注册）
     * @param envelopeXml 样本 envelope XML
     * @return 校验结果
     * @throws IOException 读取 classpath 配置失败
     */
    static ValidationResult validate(String msgNo, String envelopeXml) throws IOException {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        new ConfiguredRuleFactory(bindProductionRules(), registry).registerConfiguredRules();
        MessageType type = MessageType.byMsgNo(msgNo).orElseThrow();
        return new BusinessRuleValidator(registry)
                .validate(type, envelopeXml.getBytes(StandardCharsets.UTF_8));
    }
}
