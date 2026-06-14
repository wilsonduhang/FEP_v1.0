package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.processor.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RuleContext} — namespace-agnostic, read-only field view
 * over a parsed CFX message used by business validation rules.
 */
class RuleContextTest {

    private static RuleContext of(String xml) {
        return RuleContext.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void first_shouldReturnElementText() {
        RuleContext ctx = of("<CFX><SerialNo>SN001</SerialNo></CFX>");
        assertThat(ctx.first("SerialNo")).contains("SN001");
    }

    @Test
    void values_shouldReturnAllRepeatedElementsInDocumentOrder() {
        RuleContext ctx = of("<CFX><D><Amt>10.00</Amt></D><D><Amt>20.00</Amt></D></CFX>");
        assertThat(ctx.values("Amt")).containsExactly("10.00", "20.00");
    }

    @Test
    void values_returnedListShouldBeUnmodifiable() {
        // 契约保护（EFF-P3-1 零拷贝优化前后均须成立）：values() 返回不可修改列表，
        // 外部不得经返回值篡改内部字段视图。present 与 absent 两路均须不可修改。
        RuleContext ctx = of("<CFX><D><Amt>10.00</Amt></D></CFX>");
        assertThatThrownBy(() -> ctx.values("Amt").add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ctx.values("Missing").add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void first_shouldBeEmptyWhenFieldAbsent() {
        RuleContext ctx = of("<CFX><SerialNo>SN001</SerialNo></CFX>");
        assertThat(ctx.first("Missing")).isEmpty();
        assertThat(ctx.values("Missing")).isEmpty();
    }

    @Test
    void parse_shouldBeNamespaceAgnostic() {
        RuleContext ctx = of("<n:CFX xmlns:n=\"urn:x\"><n:SerialNo>SN9</n:SerialNo></n:CFX>");
        assertThat(ctx.first("SerialNo")).contains("SN9");
    }

    @Test
    void parse_shouldThrowOnMalformedXml() {
        assertThatThrownBy(() -> of("<CFX><unclosed></CFX>"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void parse_shouldThrowOnNullInput() {
        assertThatThrownBy(() -> RuleContext.parse(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void values_shouldMergeSameLocalNameAcrossNestingLevels() {
        // 跨层同名字段：HEAD 与 body 均有 WorkDate → 按文档顺序合并（语义明示，非 bug）
        RuleContext ctx = of("<CFX><HEAD><WorkDate>20260601</WorkDate></HEAD>"
                + "<MSG><body><WorkDate>20260605</WorkDate></body></MSG></CFX>");
        assertThat(ctx.values("WorkDate")).containsExactly("20260601", "20260605");
    }

    @Test
    void hasElement_shouldDetectContainerElementWithoutDirectText() {
        RuleContext ctx = of("<CFX><Body><RiskRate><a>1</a></RiskRate></Body></CFX>");
        assertThat(ctx.hasElement("RiskRate")).isTrue();
        assertThat(ctx.has("RiskRate")).isFalse();
    }

    @Test
    void hasElement_shouldReturnFalseForAbsentElement() {
        RuleContext ctx = of("<CFX><Body><a>1</a></Body></CFX>");
        assertThat(ctx.hasElement("RiskRate")).isFalse();
    }

    @Test
    void hasElement_shouldDetectBlankLeafElement() {
        RuleContext ctx = of("<CFX><Body><f>  </f></Body></CFX>");
        assertThat(ctx.hasElement("f")).isTrue();
        assertThat(ctx.has("f")).isFalse();
    }

    @Test
    void hasElement_nullLocalName_shouldReturnFalseLikeHas() {
        RuleContext ctx = of("<CFX><a>1</a></CFX>");
        assertThat(ctx.hasElement(null)).isFalse();
        assertThat(ctx.hasElementInHead(null)).isFalse();
    }

    @Test
    void hasElementInHead_shouldScopeToHeadSubtreeOnly() {
        RuleContext ctx = of("<CFX><HEAD><FileName>a.zip</FileName></HEAD>"
                + "<MSG><Item><FileName>b.csv</FileName><FileSize>9</FileSize></Item></MSG></CFX>");
        assertThat(ctx.hasElementInHead("FileName")).isTrue();
        assertThat(ctx.hasElementInHead("FileSize")).isFalse();
        assertThat(ctx.hasElement("FileSize")).isTrue();
    }
}
