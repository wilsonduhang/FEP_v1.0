package com.puchain.fep.processor.validation.rule;

import org.junit.jupiter.api.Test;

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
}
