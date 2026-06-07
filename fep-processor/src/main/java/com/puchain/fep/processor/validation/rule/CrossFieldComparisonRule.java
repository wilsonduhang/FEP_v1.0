package com.puchain.fep.processor.validation.rule;

import java.util.Optional;

/**
 * 跨字段比较规则：两个字段值按字符串自然序比较，不满足关系即违规。
 *
 * <p>适用于定长可比字段（如 yyyyMMdd 日期、零填充定长数值）。两字段任一缺失时不违规
 * （存在性交由 XSD / ConditionalRequiredRule）。</p>
 */
public final class CrossFieldComparisonRule implements ValidationRule {

    /** 比较算子：左字段 OP 右字段 必须成立。 */
    public enum Operator { LE, LT, GE, GT, EQ }

    private final String leftField;
    private final String rightField;
    private final Operator operator;

    /**
     * @param leftField  左字段 local-name
     * @param rightField 右字段 local-name
     * @param operator   要求成立的比较关系
     */
    public CrossFieldComparisonRule(final String leftField,
                                    final String rightField,
                                    final Operator operator) {
        this.leftField = leftField;
        this.rightField = rightField;
        this.operator = operator;
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final Optional<String> left = ctx.first(leftField);
        final Optional<String> right = ctx.first(rightField);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        final int cmp = left.get().compareTo(right.get());
        final boolean ok = switch (operator) {
            case LE -> cmp <= 0;
            case LT -> cmp < 0;
            case GE -> cmp >= 0;
            case GT -> cmp > 0;
            case EQ -> cmp == 0;
        };
        return ok ? Optional.empty()
                : Optional.of("字段 " + leftField + " 与 " + rightField
                        + " 不满足 " + operator + " 关系");
    }
}
