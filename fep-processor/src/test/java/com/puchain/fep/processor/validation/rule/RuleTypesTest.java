package com.puchain.fep.processor.validation.rule;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the three reusable {@link ValidationRule} types:
 * {@link ConditionalRequiredRule}, {@link CrossFieldComparisonRule},
 * {@link EnumMembershipRule}.
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
}
