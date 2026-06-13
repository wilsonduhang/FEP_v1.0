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

    /** 生产 yaml 不可变 → 绑定结果与校验器全套静态缓存（EFF：消除每用例重复解析/重建，实测单类 ~107s→秒级）。 */
    private static BusinessRuleValidator cachedValidator;

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
        MessageType type = MessageType.byMsgNo(msgNo).orElseThrow();
        return validator().validate(type, envelopeXml.getBytes(StandardCharsets.UTF_8));
    }

    private static synchronized BusinessRuleValidator validator() throws IOException {
        if (cachedValidator == null) {
            MessageRuleRegistry registry = new MessageRuleRegistry();
            new ConfiguredRuleFactory(bindProductionRules(), registry).registerConfiguredRules();
            cachedValidator = new BusinessRuleValidator(registry);
        }
        return cachedValidator;
    }

    /** 构造单字段最小 envelope（共享自 BatchTransfer/SupplyChain 测试，REUSE F1 下沉）。 */
    static String envelope(String msgNo, String body, String field, String value) {
        return "<CFX><HEAD><MsgNo>" + msgNo + "</MsgNo></HEAD><MSG><" + body + ">"
                + "<" + field + ">" + value + "</" + field + ">"
                + "</" + body + "></MSG></CFX>";
    }

    /** 合法值通过 + 非法值违规（含字段名）成对断言（REUSE F1 下沉）。 */
    static void assertRule(String msgNo, String body, String field,
                           String legal, String illegal) throws IOException {
        org.assertj.core.api.Assertions.assertThat(validate(msgNo,
                envelope(msgNo, body, field, legal)).valid())
                .as("%s %s=%s legal", msgNo, field, legal).isTrue();
        ValidationResult bad = validate(msgNo, envelope(msgNo, body, field, illegal));
        org.assertj.core.api.Assertions.assertThat(bad.valid())
                .as("%s %s=%s illegal", msgNo, field, illegal).isFalse();
        org.assertj.core.api.Assertions.assertThat(String.join(";", bad.errors()))
                .contains(field);
    }
}
