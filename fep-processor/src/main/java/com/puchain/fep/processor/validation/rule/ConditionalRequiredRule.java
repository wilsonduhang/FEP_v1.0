package com.puchain.fep.processor.validation.rule;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * 条件必填规则：当触发字段满足条件时，目标字段必须有值。
 *
 * <p>典型用途：回执报文 ResultCode≠成功码 时 ResultMsg 必填——XSD 无法表达此条件依赖。</p>
 */
public final class ConditionalRequiredRule implements ValidationRule {

    private final String targetField;
    private final String triggerField;
    private final Predicate<String> triggerCondition;

    /**
     * @param targetField      条件成立时必填的字段 local-name
     * @param triggerField     触发字段 local-name
     * @param triggerCondition 对触发字段值的条件；为 true 时要求 targetField 必填
     */
    public ConditionalRequiredRule(final String targetField,
                                   final String triggerField,
                                   final Predicate<String> triggerCondition) {
        this.targetField = targetField;
        this.triggerField = triggerField;
        this.triggerCondition = triggerCondition;
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final Optional<String> trigger = ctx.first(triggerField);
        if (trigger.isEmpty() || !triggerCondition.test(trigger.get())) {
            return Optional.empty();
        }
        if (ctx.has(targetField)) {
            return Optional.empty();
        }
        return Optional.of("字段 " + targetField + " 在 " + triggerField + " 触发条件下必填");
    }
}
