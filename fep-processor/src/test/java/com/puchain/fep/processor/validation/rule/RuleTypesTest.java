package com.puchain.fep.processor.validation.rule;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the reusable {@link ValidationRule} types:
 * {@link ConditionalRequiredRule}, {@link CrossFieldComparisonRule},
 * {@link EnumMembershipRule}, {@link DependentEnumRule}, {@link GroupCooccurrenceRule}.
 */
class RuleTypesTest {

    private static RuleContext ctx(String xml) {
        return RuleContext.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void conditionalRequired_violatedWhenTriggerMatchesAndTargetMissing() {
        ValidationRule rule = new ConditionalRequiredRule(
                "ResultMsg", "ResultCode", v -> !"0000".equals(v));
        // 违规描述须含目标字段名（验收标准 5：可定位）
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>9999</ResultCode></CFX>")))
                .get().asString().contains("ResultMsg");
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>0000</ResultCode></CFX>")))
                .isEmpty();
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>9999</ResultCode><ResultMsg>err</ResultMsg></CFX>")))
                .isEmpty();
    }

    @Test
    void crossFieldComparison_violatedWhenBeginAfterEnd() {
        ValidationRule rule = new CrossFieldComparisonRule(
                "BeginDate", "EndDate", CrossFieldComparisonRule.Operator.LE);
        assertThat(rule.evaluate(ctx("<CFX><BeginDate>20260605</BeginDate><EndDate>20260601</EndDate></CFX>")))
                .get().asString().contains("BeginDate", "EndDate");
        assertThat(rule.evaluate(ctx("<CFX><BeginDate>20260601</BeginDate><EndDate>20260605</EndDate></CFX>")))
                .isEmpty();
    }

    @Test
    void enumMembership_violatedWhenValueNotInSet() {
        ValidationRule rule = new EnumMembershipRule("Currency", Set.of("CNY", "USD"));
        assertThat(rule.evaluate(ctx("<CFX><Currency>JPY</Currency></CFX>")))
                .get().asString().contains("Currency");
        assertThat(rule.evaluate(ctx("<CFX><Currency>CNY</Currency></CFX>"))).isEmpty();
        assertThat(rule.evaluate(ctx("<CFX><Other>x</Other></CFX>"))).isEmpty(); // 缺失不违规
    }

    @Test
    void dependentEnum_violatedWhenTargetNotInKeyedSet() {
        ValidationRule rule = new DependentEnumRule(
                "SecondClass", "MainClass",
                Map.of("EAST", List.of("V50")));
        // 验收 1：key 命中且目标合法 → 通过
        assertThat(rule.evaluate(ctx("<CFX><MainClass>EAST</MainClass><SecondClass>V50</SecondClass></CFX>")))
                .isEmpty();
        // 验收 2：key 命中且目标非法 → 违规，含目标字段名
        assertThat(rule.evaluate(ctx("<CFX><MainClass>EAST</MainClass><SecondClass>HX01</SecondClass></CFX>")))
                .get().asString().contains("SecondClass");
        // 验收 3：key 未映射（GENERAL 自由）→ 不约束
        assertThat(rule.evaluate(ctx("<CFX><MainClass>GENERAL</MainClass><SecondClass>anything</SecondClass></CFX>")))
                .isEmpty();
        // 验收 4：key 缺失 → 通过
        assertThat(rule.evaluate(ctx("<CFX><SecondClass>V50</SecondClass></CFX>"))).isEmpty();
        // 验收 5：目标缺失 → 通过
        assertThat(rule.evaluate(ctx("<CFX><MainClass>EAST</MainClass></CFX>"))).isEmpty();
    }

    @Test
    void dependentEnum_defensivelyCopiesConstructorMaps() {
        java.util.Map<String, java.util.List<String>> src = new java.util.HashMap<>();
        java.util.List<String> vals = new java.util.ArrayList<>();
        vals.add("V50");
        src.put("EAST", vals);
        ValidationRule rule = new DependentEnumRule("SecondClass", "MainClass", src);
        // 验收 6：构造后外部修改不影响规则
        vals.add("HX01");
        src.clear();
        assertThat(rule.evaluate(ctx("<CFX><MainClass>EAST</MainClass><SecondClass>HX01</SecondClass></CFX>")))
                .get().asString().contains("SecondClass");
    }
}
