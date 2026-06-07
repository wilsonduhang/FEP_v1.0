package com.puchain.fep.processor.validation.rule;

import java.util.Optional;
import java.util.Set;

/**
 * 枚举/码表成员规则：字段值必须属于允许集合。字段缺失时不违规（必填另行规则）。
 *
 * <p>用于 XSD 未约束或需运行时码表（按银行差异化）的枚举校验。</p>
 */
public final class EnumMembershipRule implements ValidationRule {

    private final String field;
    private final Set<String> allowed;

    /**
     * @param field   字段 local-name
     * @param allowed 允许值集合（不可为空）
     */
    public EnumMembershipRule(final String field, final Set<String> allowed) {
        this.field = field;
        this.allowed = Set.copyOf(allowed);
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final Optional<String> value = ctx.first(field);
        if (value.isEmpty() || allowed.contains(value.get())) {
            return Optional.empty();
        }
        return Optional.of("字段 " + field + " 值 [" + value.get() + "] 不在允许集合 " + allowed);
    }
}
