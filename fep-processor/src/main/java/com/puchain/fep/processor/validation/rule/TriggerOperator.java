package com.puchain.fep.processor.validation.rule;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 条件必填规则（{@link ConditionalRequiredRule}）的触发算子：由配置算子与值集构建对触发字段值的谓词。
 *
 * <p>谓词返回 {@code true} 表示触发条件成立，要求目标字段必填——XSD 无法表达此方向感知的条件依赖
 * （如 {@code ResultCode NOT_IN {0,00} → ResultMsg 必填}）。值精确字符串比对（trim 由
 * {@link RuleContext} 解析期完成）。</p>
 *
 * <p>算子机制由 AI 编码，算子与值由领域专家按人行规范在配置中定义（mode C），
 * 启动期 {@link ConfiguredRuleFactory} 装配，配置非法即 fail-fast。</p>
 */
public enum TriggerOperator {

    /** 触发字段值等于配置单值（triggerValues 须恰 1 个）。 */
    EQUALS,
    /** 触发字段值不等于配置单值（triggerValues 须恰 1 个）。 */
    NOT_EQUALS,
    /** 触发字段值属于配置值集（triggerValues 须 ≥ 1 个）。 */
    IN,
    /** 触发字段值不属于配置值集（triggerValues 须 ≥ 1 个）。 */
    NOT_IN;

    /**
     * 由本算子与配置值集构建对触发字段值的谓词。
     *
     * @param triggerValues 配置值集；EQUALS/NOT_EQUALS 须恰 1 个，IN/NOT_IN 须 ≥ 1 个
     * @return 谓词：返回 true 表示触发条件成立（要求目标字段必填）
     * @throws IllegalArgumentException triggerValues 个数不满足本算子约束
     */
    public Predicate<String> toPredicate(final List<String> triggerValues) {
        return switch (this) {
            case EQUALS -> {
                final String single = requireSingle(triggerValues);
                yield single::equals;
            }
            case NOT_EQUALS -> {
                final String single = requireSingle(triggerValues);
                yield v -> !single.equals(v);
            }
            case IN -> {
                final Set<String> set = requireNonEmptySet(triggerValues);
                yield set::contains;
            }
            case NOT_IN -> {
                final Set<String> set = requireNonEmptySet(triggerValues);
                yield v -> !set.contains(v);
            }
        };
    }

    private String requireSingle(final List<String> values) {
        if (values == null || values.size() != 1) {
            throw new IllegalArgumentException(
                    name() + " 触发算子要求恰 1 个 triggerValues，实际: "
                            + (values == null ? "null" : values.size()));
        }
        return values.get(0);
    }

    private Set<String> requireNonEmptySet(final List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(
                    name() + " 触发算子要求至少 1 个 triggerValues");
        }
        return Set.copyOf(values); // 防御拷贝 + 去重
    }
}
