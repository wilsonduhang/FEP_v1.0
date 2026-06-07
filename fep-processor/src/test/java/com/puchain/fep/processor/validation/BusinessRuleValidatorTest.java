package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.rule.EnumMembershipRule;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link BusinessRuleValidator} — orchestrates per-MessageType
 * business rules registered in {@link MessageRuleRegistry} and aggregates all
 * violations (collecting, not short-circuiting).
 */
class BusinessRuleValidatorTest {

    private static byte[] xml(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // A real synchronous message type (mirrors SyncMessageProcessorServiceTest fixtures).
    private final MessageType sampleType = MessageType.MSG_1001;

    @Test
    void validate_shouldReturnOkWhenNoRulesRegistered() {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        BusinessRuleValidator v = new BusinessRuleValidator(registry);
        assertThat(v.validate(sampleType, xml("<CFX><Currency>JPY</Currency></CFX>")).valid())
                .isTrue();
    }

    @Test
    void validate_shouldCollectAllViolations() {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        registry.register(sampleType, new EnumMembershipRule("Currency", Set.of("CNY")));
        registry.register(sampleType, new EnumMembershipRule("Status", Set.of("1")));
        BusinessRuleValidator v = new BusinessRuleValidator(registry);

        ValidationResult r = v.validate(sampleType,
                xml("<CFX><Currency>JPY</Currency><Status>9</Status></CFX>"));

        assertThat(r.valid()).isFalse();
        assertThat(r.errors()).hasSize(2);
    }

    @Test
    void validate_shouldPassWhenAllRulesSatisfied() {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        registry.register(sampleType, new EnumMembershipRule("Currency", Set.of("CNY")));
        BusinessRuleValidator v = new BusinessRuleValidator(registry);
        assertThat(v.validate(sampleType, xml("<CFX><Currency>CNY</Currency></CFX>")).valid())
                .isTrue();
    }

    @Test
    void validate_singleViolation_errorsShouldContainRuleDescription() {
        // 验收标准 3：单规则违规 → failed，errors 含该规则的错误描述（含字段名）
        MessageRuleRegistry registry = new MessageRuleRegistry();
        registry.register(sampleType, new EnumMembershipRule("Currency", Set.of("CNY")));
        BusinessRuleValidator v = new BusinessRuleValidator(registry);

        ValidationResult r = v.validate(sampleType, xml("<CFX><Currency>JPY</Currency></CFX>"));

        assertThat(r.valid()).isFalse();
        assertThat(r.errors()).hasSize(1);
        assertThat(r.errors().get(0)).contains("Currency");
    }

    @Test
    void validate_shouldThrowValidationExceptionOnMalformedXml() {
        // 验收标准 5：注册规则后遇非良构 XML → 抛 ValidationException（不静默吞，由 pipeline 处理）
        MessageRuleRegistry registry = new MessageRuleRegistry();
        registry.register(sampleType, new EnumMembershipRule("Currency", Set.of("CNY")));
        BusinessRuleValidator v = new BusinessRuleValidator(registry);

        assertThatThrownBy(() -> v.validate(sampleType, xml("<CFX><unclosed></CFX>")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void registry_rulesFor_shouldReturnRegisteredRulesAndEmptyForUnknownType() {
        // 验收标准 1：register 后 rulesFor 含该规则；未注册类型 → 空 List
        MessageRuleRegistry registry = new MessageRuleRegistry();
        EnumMembershipRule rule = new EnumMembershipRule("Currency", Set.of("CNY"));
        registry.register(sampleType, rule);

        assertThat(registry.rulesFor(sampleType)).containsExactly(rule);
        // 另取一个未注册的报文类型 → 空 List
        MessageType other = MessageType.MSG_2001;
        assertThat(registry.rulesFor(other)).isEmpty();
    }

    @Test
    void registry_rulesFor_shouldReturnUnmodifiableView() {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        registry.register(sampleType, new EnumMembershipRule("Currency", Set.of("CNY")));
        List<?> view = registry.rulesFor(sampleType);
        assertThatThrownBy(() -> view.clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
