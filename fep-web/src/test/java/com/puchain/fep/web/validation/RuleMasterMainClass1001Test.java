package com.puchain.fep.web.validation;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.BusinessRuleValidator;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.rule.ConfiguredRuleFactory;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import com.puchain.fep.processor.validation.rule.RuleDefinitionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * §5.8 规则母本 mode C — R1：业务大类 {@code MainClass} ENUM 校验（报文 1001）。
 *
 * <p>权威源：《接口报文规范 V2.0.0》§5.1.3 表 5.1.3-1 业务类别代码规范（10 大类，p199）+
 * §3.1.1.2 表 3.1.1.2-2 RealHead1001（MainClass 标记 M*，枚举型）。XSD 仅约束
 * {@code MainClass} 为 Token minLength=2/maxLength=16（DataType.xsd），不强制枚举成员，
 * 故该业务规则由引擎补足。</p>
 *
 * <p>本测试直接绑定生产 {@code application.yml} 的 {@code fep.validation} 段，验证生产配置
 * 正确装配为规则并在真实 1001 报文上生效——不拉起完整 Spring 上下文（避 fep-web 重 IT 成本）。</p>
 */
class RuleMasterMainClass1001Test {

    /** §5.1.3-1 业务大类代码（10 大类，完整枚举）。 */
    private static final List<String> MAIN_CLASS_CODES = List.of(
            "EAST", "COINFO", "COAUTH", "GYL", "MONITOR",
            "STATS", "YWTB", "ZFJS", "SYSTEM", "GENERAL");

    private static RuleDefinitionProperties bindProductionRules() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources =
                loader.load("application", new ClassPathResource("application.yml"));
        StandardEnvironment env = new StandardEnvironment();
        sources.forEach(env.getPropertySources()::addFirst);
        return Binder.get(env).bind("fep.validation", RuleDefinitionProperties.class)
                .orElseGet(RuleDefinitionProperties::new);
    }

    private static byte[] msg1001(String mainClass) {
        String xml = "<CFX><MSG><RealHead1001>"
                + "<TransitionNo>20260608</TransitionNo>"
                + "<MainClass>" + mainClass + "</MainClass>"
                + "<SecondClass>I1001</SecondClass>"
                + "</RealHead1001></MSG></CFX>";
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void productionConfig_shouldBindMainClassEnumRuleFor1001() throws IOException {
        RuleDefinitionProperties props = bindProductionRules();

        List<RuleDefinitionProperties.RuleDef> defs = props.getRules().get("1001");
        assertThat(defs).hasSize(2);
        RuleDefinitionProperties.RuleDef enumDef = defs.stream()
                .filter(d -> "ENUM".equals(d.getType())).findFirst().orElseThrow();
        assertThat(enumDef.getField()).isEqualTo("MainClass");
        assertThat(enumDef.getAllowed()).containsExactlyInAnyOrderElementsOf(MAIN_CLASS_CODES);
    }

    @Test
    void mainClassRule_shouldRejectUnknownBusinessCategory() throws IOException {
        BusinessRuleValidator validator = validatorFromProductionConfig();
        ValidationResult r = validator.validate(MessageType.MSG_1001, msg1001("ZZZZ"));
        assertThat(r.valid()).isFalse();
        assertThat(r.errors().get(0)).contains("MainClass");
    }

    @Test
    void mainClassRule_shouldAcceptKnownBusinessCategory() throws IOException {
        BusinessRuleValidator validator = validatorFromProductionConfig();
        ValidationResult r = validator.validate(MessageType.MSG_1001, msg1001("COINFO"));
        assertThat(r.valid()).isTrue();
    }

    /** 显式 MainClass + SecondClass 的 1001 envelope（SecondClass 依赖枚举测试用）。 */
    private static byte[] msg1001(String mainClass, String secondClass) {
        String xml = "<CFX><MSG><RealHead1001>"
                + "<TransitionNo>20260610</TransitionNo>"
                + "<MainClass>" + mainClass + "</MainClass>"
                + "<SecondClass>" + secondClass + "</SecondClass>"
                + "</RealHead1001></MSG></CFX>";
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void productionConfig_shouldBindSecondClassDependentEnumRuleFor1001() throws IOException {
        RuleDefinitionProperties props = bindProductionRules();
        RuleDefinitionProperties.RuleDef depDef = props.getRules().get("1001").stream()
                .filter(d -> "DEPENDENT_ENUM".equals(d.getType())).findFirst().orElseThrow();
        assertThat(depDef.getField()).isEqualTo("SecondClass");
        assertThat(depDef.getKeyField()).isEqualTo("MainClass");
        // §5.1.3-1：9 个非-GENERAL 大类映射；GENERAL 故意省略（不约束）
        assertThat(depDef.getAllowedByKey()).hasSize(9)
                .containsKeys("EAST", "COINFO", "COAUTH", "GYL", "MONITOR",
                        "STATS", "YWTB", "ZFJS", "SYSTEM")
                .doesNotContainKey("GENERAL");
        assertThat(depDef.getAllowedByKey().get("GYL"))
                .containsExactlyInAnyOrder("HX01", "HX02", "HX03", "HX04",
                        "PT01", "PT02", "PT03", "BB01", "BB02");
    }

    @Test
    void secondClassRule_shouldAcceptCodeInKeyedSet() throws IOException {
        BusinessRuleValidator validator = validatorFromProductionConfig();
        // 验收 2：COINFO + I1001 合法
        assertThat(validator.validate(MessageType.MSG_1001, msg1001("COINFO", "I1001")).valid()).isTrue();
        // 验收 4：GYL + PT03 合法
        assertThat(validator.validate(MessageType.MSG_1001, msg1001("GYL", "PT03")).valid()).isTrue();
        // 验收 6：EAST + V50 合法
        assertThat(validator.validate(MessageType.MSG_1001, msg1001("EAST", "V50")).valid()).isTrue();
    }

    @Test
    void secondClassRule_shouldRejectCodeNotInKeyedSet() throws IOException {
        BusinessRuleValidator validator = validatorFromProductionConfig();
        // 验收 3：COINFO + V50（V50 仅属 EAST）→ invalid
        ValidationResult r = validator.validate(MessageType.MSG_1001, msg1001("COINFO", "V50"));
        assertThat(r.valid()).isFalse();
        assertThat(r.errors().get(0)).contains("SecondClass");
        // 验收 6：EAST + I1001（I1001 不属 EAST）→ invalid
        assertThat(validator.validate(MessageType.MSG_1001, msg1001("EAST", "I1001")).valid()).isFalse();
    }

    @Test
    void secondClassRule_shouldNotConstrainGeneralFreeClass() throws IOException {
        BusinessRuleValidator validator = validatorFromProductionConfig();
        // 验收 5：GENERAL 未映射 → 任意小类不约束
        assertThat(validator.validate(MessageType.MSG_1001, msg1001("GENERAL", "FREEDEF")).valid()).isTrue();
    }

    /** 从生产 application.yml 装配 BusinessRuleValidator（仅 1001 规则注册）。 */
    private static BusinessRuleValidator validatorFromProductionConfig() throws IOException {
        RuleDefinitionProperties props = bindProductionRules();
        MessageRuleRegistry registry = new MessageRuleRegistry();
        props.getRules().getOrDefault("1001", List.of())
                .forEach(def -> registry.register(MessageType.MSG_1001, ConfiguredRuleFactory.build(def)));
        return new BusinessRuleValidator(registry);
    }
}
