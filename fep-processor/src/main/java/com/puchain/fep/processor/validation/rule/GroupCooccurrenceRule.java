package com.puchain.fep.processor.validation.rule;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 分组可选共现规则：一组字段必须同时使用或同时不使用。
 *
 * <p>典型用途：报文规范 §2.1.3.1 分组字段"同时使用或同时不使用"——XSD 表达不了
 * 这种跨字段共现约束。"使用"按元素出现判定（{@link RuleContext#hasElement}，
 * 容器元素亦可探测）；{@link Scope#HEAD} 作用域时限报文头 HEAD 子树
 * （{@link RuleContext#hasElementInHead}），避免与 body 内同名元素串扰。</p>
 */
public final class GroupCooccurrenceRule implements ValidationRule {

    /** 分组探测作用域。 */
    public enum Scope {
        /** 全报文（默认）。 */
        MESSAGE,
        /** 仅报文头 HEAD 子树（报文规范表 2.2.2.2-1）。 */
        HEAD
    }

    private final List<String> groupFields;
    private final Scope scope;

    /**
     * 全报文作用域分组规则。
     *
     * @param groupFields 分组字段 local-name 列表，至少 2 个
     * @throws IllegalArgumentException 分组少于 2 个字段
     */
    public GroupCooccurrenceRule(final List<String> groupFields) {
        this(groupFields, Scope.MESSAGE);
    }

    /**
     * 指定作用域的分组规则。
     *
     * @param groupFields 分组字段 local-name 列表，至少 2 个
     * @param scope       探测作用域，非空
     * @throws IllegalArgumentException 分组少于 2 个字段或 scope 为空
     */
    public GroupCooccurrenceRule(final List<String> groupFields, final Scope scope) {
        if (groupFields == null || groupFields.size() < 2) {
            throw new IllegalArgumentException("分组共现规则至少需要 2 个字段");
        }
        if (scope == null) {
            throw new IllegalArgumentException("分组共现规则 scope 不能为空");
        }
        this.groupFields = List.copyOf(groupFields);
        this.scope = scope;
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final Predicate<String> used = scope == Scope.HEAD
                ? ctx::hasElementInHead : ctx::hasElement;
        final List<String> present = groupFields.stream().filter(used).toList();
        if (present.isEmpty() || present.size() == groupFields.size()) {
            return Optional.empty();
        }
        final List<String> missing = groupFields.stream()
                .filter(f -> !used.test(f)).toList();
        return Optional.of("分组字段 " + groupFields + " 须同时使用或同时不使用；已填 "
                + present + " 缺失 " + missing);
    }
}
