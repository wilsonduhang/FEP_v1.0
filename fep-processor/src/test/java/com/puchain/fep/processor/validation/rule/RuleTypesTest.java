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
    void enumMembership_shouldValidateEveryOccurrenceNotJustFirst() {
        // Plan Task 3 验收 1：嵌套重复项第 2 个非法值须被抓
        ValidationRule rule = new EnumMembershipRule("MainClass", Set.of("GYL", "EAST"));
        assertThat(rule.evaluate(ctx("<CFX><Body><Item><MainClass>GYL</MainClass></Item>"
                + "<Item><MainClass>BAD</MainClass></Item></Body></CFX>")))
                .get().asString().contains("BAD");
        // Plan Task 3 验收 2：全部值合法 → 通过
        assertThat(rule.evaluate(ctx("<CFX><Body><Item><MainClass>GYL</MainClass></Item>"
                + "<Item><MainClass>EAST</MainClass></Item></Body></CFX>"))).isEmpty();
    }

    @Test
    void enumMembership_shouldListAllIllegalValuesInOneMessage() {
        // Plan Task 3 验收 3：多个非法值一条违规消息列全
        ValidationRule rule = new EnumMembershipRule("Currency", Set.of("CNY"));
        assertThat(rule.evaluate(ctx("<CFX><D><Currency>JPY</Currency></D>"
                + "<D><Currency>KRW</Currency></D></CFX>")))
                .get().asString().contains("JPY", "KRW");
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

    @Test
    void groupCooccurrence_violatedWhenPartiallyPresent() {
        ValidationRule rule = new GroupCooccurrenceRule(List.of("AcctNo", "AcctName", "BankNo"));
        // 验收 1：全部有值 → 通过
        assertThat(rule.evaluate(ctx(
                "<CFX><AcctNo>123</AcctNo><AcctName>n</AcctName><BankNo>9</BankNo></CFX>"))).isEmpty();
        // 验收 2：全部无值 → 通过
        assertThat(rule.evaluate(ctx("<CFX><Other>x</Other></CFX>"))).isEmpty();
        // 验收 3：部分有值 → 违规，含缺失字段名
        assertThat(rule.evaluate(ctx("<CFX><AcctNo>123</AcctNo></CFX>")))
                .get().asString().contains("AcctName", "BankNo");
    }

    @Test
    void groupCooccurrence_shouldDetectContainerElementAsUsed() {
        // Plan Task 2 验收 1/2：容器元素（无直接文本）按元素存在判定
        ValidationRule rule = new GroupCooccurrenceRule(List.of("RiskRate", "edUpdateDateTime"));
        assertThat(rule.evaluate(ctx("<CFX><Body><RiskRate><a>1</a></RiskRate></Body></CFX>")))
                .get().asString().contains("edUpdateDateTime");
        assertThat(rule.evaluate(ctx("<CFX><Body><RiskRate><a>1</a></RiskRate>"
                + "<edUpdateDateTime>20260611120000</edUpdateDateTime></Body></CFX>")))
                .isEmpty();
    }

    @Test
    void groupCooccurrence_headScope_shouldIgnoreBodySameNameElements() {
        // Plan Task 2 验收 4：scope=HEAD 防 body 同名串扰（1102 核对项 FileName 形态）
        ValidationRule rule = new GroupCooccurrenceRule(
                List.of("FileName", "FileContentHash", "FileSize"), GroupCooccurrenceRule.Scope.HEAD);
        assertThat(rule.evaluate(ctx("<CFX><HEAD><MsgNo>1102</MsgNo></HEAD>"
                + "<MSG><Item><FileName>old.csv</FileName></Item></MSG></CFX>"))).isEmpty();
        assertThat(rule.evaluate(ctx("<CFX><HEAD><FileName>a.zip</FileName><FileSize>10</FileSize></HEAD>"
                + "<MSG><Body>x</Body></MSG></CFX>")))
                .get().asString().contains("FileContentHash");
    }

    @Test
    void groupCooccurrence_rejectsGroupSmallerThanTwo() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> new GroupCooccurrenceRule(List.of("Only")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void groupCooccurrence_defensivelyCopiesConstructorList() {
        java.util.List<String> fields = new java.util.ArrayList<>();
        fields.add("AcctNo");
        fields.add("AcctName");
        ValidationRule rule = new GroupCooccurrenceRule(fields);
        // 验收 6：构造后外部增删不影响规则
        fields.add("BankNo");
        assertThat(rule.evaluate(ctx("<CFX><AcctNo>1</AcctNo><AcctName>n</AcctName></CFX>"))).isEmpty();
    }
}
