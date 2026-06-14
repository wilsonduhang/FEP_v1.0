package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ConfiguredRuleFactory} — builds {@link ValidationRule}
 * instances from {@link RuleDefinitionProperties.RuleDef} and registers them.
 */
class ConfiguredRuleFactoryTest {

    private static RuleDefinitionProperties.RuleDef enumDef() {
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("ENUM");
        def.setField("Currency");
        def.setAllowed(List.of("CNY", "USD"));
        return def;
    }

    @Test
    void build_shouldCreateEnumRuleFromDefinition() {
        ValidationRule rule = ConfiguredRuleFactory.build(enumDef());
        assertThat(rule).isInstanceOf(EnumMembershipRule.class);
    }

    @Test
    void build_shouldCreateCrossFieldRuleFromDefinition() {
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("CROSS_FIELD");
        def.setField("BeginDate");
        def.setCompareField("EndDate");
        def.setOperator("LE");
        assertThat(ConfiguredRuleFactory.build(def)).isInstanceOf(CrossFieldComparisonRule.class);
    }

    @Test
    void build_shouldCreateDependentEnumRuleFromDefinition() {
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("DEPENDENT_ENUM");
        def.setField("SecondClass");
        def.setKeyField("MainClass");
        def.setAllowedByKey(Map.of("EAST", List.of("V50")));
        assertThat(ConfiguredRuleFactory.build(def)).isInstanceOf(DependentEnumRule.class);
    }

    @Test
    void build_shouldCreateGroupCooccurrenceRuleFromDefinition() {
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("GROUP_COOCCURRENCE");
        def.setGroupFields(List.of("AcctNo", "AcctName"));
        assertThat(ConfiguredRuleFactory.build(def)).isInstanceOf(GroupCooccurrenceRule.class);
    }

    @Test
    void build_shouldThrowOnUnknownRuleType() {
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("NOPE");
        assertThatThrownBy(() -> ConfiguredRuleFactory.build(def))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知规则类型");
    }

    @Test
    void registerConfiguredRules_shouldRegisterToRegistry() {
        RuleDefinitionProperties props = new RuleDefinitionProperties();
        props.setRules(Map.of("3116", List.of(enumDef())));
        MessageRuleRegistry registry = new MessageRuleRegistry();
        new ConfiguredRuleFactory(props, registry).registerConfiguredRules();

        assertThat(registry.rulesFor(com.puchain.fep.converter.type.MessageType.MSG_3116))
                .hasSize(1);
    }

    @Test
    void registerConfiguredRules_shouldThrowOnUnknownMsgNo() {
        RuleDefinitionProperties props = new RuleDefinitionProperties();
        props.setRules(Map.of("0000", List.of(enumDef())));
        ConfiguredRuleFactory factory = new ConfiguredRuleFactory(props, new MessageRuleRegistry());
        assertThatThrownBy(factory::registerConfiguredRules)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知报文号");
    }

    private static RuleDefinitionProperties.RuleDef headGroupDef() {
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("GROUP_COOCCURRENCE");
        def.setGroupFields(List.of("FileName", "FileContentHash", "FileSize"));
        def.setScope("HEAD");
        return def;
    }

    @Test
    void wildcardMsgNo_shouldRegisterRuleForAllMessageTypes() {
        // Plan Task 5 验收 1：通配键 → 全部 MessageType 各注册 1 条
        RuleDefinitionProperties props = new RuleDefinitionProperties();
        props.setRules(Map.of("*", List.of(headGroupDef())));
        MessageRuleRegistry registry = new MessageRuleRegistry();
        new ConfiguredRuleFactory(props, registry).registerConfiguredRules();
        for (MessageType type : MessageType.values()) {
            assertThat(registry.rulesFor(type)).as("type %s", type).hasSize(1);
        }
    }

    @Test
    void wildcardAndSpecificMsgNo_shouldAggregate() {
        // Plan Task 5 验收 2：通配与具体 msgNo 并存 → 具体报文聚合两者
        RuleDefinitionProperties props = new RuleDefinitionProperties();
        props.setRules(Map.of("*", List.of(headGroupDef()), "3116", List.of(enumDef())));
        MessageRuleRegistry registry = new MessageRuleRegistry();
        new ConfiguredRuleFactory(props, registry).registerConfiguredRules();
        assertThat(registry.rulesFor(MessageType.MSG_3116)).hasSize(2);
    }

    @Test
    void wildcardYamlBracketKey_shouldBindLiteralStarThroughRelaxedBinding() {
        // Plan 决策 6 防回归：经真实 Binder 验证 "[*]" 绑定为字面 "*" key（裸 * 会被 relaxed binding 剥除）
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("t", Map.of(
                "fep.validation.rules.[*][0].type", "GROUP_COOCCURRENCE",
                "fep.validation.rules.[*][0].scope", "HEAD",
                "fep.validation.rules.[*][0].group-fields[0]", "FileName",
                "fep.validation.rules.[*][0].group-fields[1]", "FileContentHash",
                "fep.validation.rules.[*][0].group-fields[2]", "FileSize")));
        RuleDefinitionProperties bound = Binder.get(env)
                .bind("fep.validation", RuleDefinitionProperties.class).get();
        assertThat(bound.getRules()).containsKey("*");
        assertThat(bound.getRules().get("*").get(0).getScope()).isEqualTo("HEAD");
    }

    @Test
    void build_groupCooccurrence_shouldPassScopeThrough() {
        // Task 2 review minor 3：scope="HEAD" 透传 + 非法 scope fail-fast
        assertThat(ConfiguredRuleFactory.build(headGroupDef()))
                .isInstanceOf(GroupCooccurrenceRule.class);
        RuleDefinitionProperties.RuleDef bad = headGroupDef();
        bad.setScope("head");
        assertThatThrownBy(() -> ConfiguredRuleFactory.build(bad))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Phase 3: CONDITIONAL_REQUIRED direction-aware trigger predicate ──

    private static RuleContext ctx(String xml) {
        return RuleContext.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static RuleDefinitionProperties.RuleDef conditionalDef(String operator, List<String> values) {
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("CONDITIONAL_REQUIRED");
        def.setField("ResultMsg");
        def.setTriggerField("ResultCode");
        def.setTriggerOperator(operator);
        def.setTriggerValues(values);
        return def;
    }

    @Test
    void build_conditionalRequired_withNotInOperator_evaluatesDirectionAware() {
        // 验收 1：行为级 — 谓词正确接线（NOT_IN {0,00}）
        ValidationRule rule = ConfiguredRuleFactory.build(conditionalDef("NOT_IN", List.of("0", "00")));
        assertThat(rule).isInstanceOf(ConditionalRequiredRule.class);
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>99</ResultCode></CFX>")))
                .get().asString().contains("ResultMsg");
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>0</ResultCode></CFX>"))).isEmpty();
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>99</ResultCode><ResultMsg>x</ResultMsg></CFX>")))
                .isEmpty();
    }

    @Test
    void build_conditionalRequired_withoutOperator_keepsLegacyPresenceBehavior() {
        // 验收 2：operator 缺省 → legacy「触发字段存在即要求目标」
        ValidationRule rule = ConfiguredRuleFactory.build(conditionalDef(null, List.of()));
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>0</ResultCode></CFX>")))
                .get().asString().contains("ResultMsg");
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>0</ResultCode><ResultMsg>x</ResultMsg></CFX>")))
                .isEmpty();
    }

    @Test
    void build_conditionalRequired_equalsWithTwoValues_failsFast() {
        // 验收 3：透传 TriggerOperator fail-fast
        assertThatThrownBy(() -> ConfiguredRuleFactory.build(conditionalDef("EQUALS", List.of("A", "B"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_conditionalRequired_unknownOperator_failsFast() {
        // 验收 4：非法 operator
        assertThatThrownBy(() -> ConfiguredRuleFactory.build(conditionalDef("BOGUS", List.of("0"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conditionalRequired_relaxedBinding_bindsOperatorAndValues() {
        // 验收 5：kebab-case trigger-operator/trigger-values 经 Binder 绑定
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("t", Map.of(
                "fep.validation.rules.3009[0].type", "CONDITIONAL_REQUIRED",
                "fep.validation.rules.3009[0].field", "ResultMsg",
                "fep.validation.rules.3009[0].trigger-field", "ResultCode",
                "fep.validation.rules.3009[0].trigger-operator", "NOT_IN",
                "fep.validation.rules.3009[0].trigger-values[0]", "0",
                "fep.validation.rules.3009[0].trigger-values[1]", "00")));
        RuleDefinitionProperties bound = Binder.get(env)
                .bind("fep.validation", RuleDefinitionProperties.class).get();
        RuleDefinitionProperties.RuleDef def = bound.getRules().get("3009").get(0);
        assertThat(def.getTriggerOperator()).isEqualTo("NOT_IN");
        assertThat(def.getTriggerValues()).containsExactly("0", "00");
    }
}
