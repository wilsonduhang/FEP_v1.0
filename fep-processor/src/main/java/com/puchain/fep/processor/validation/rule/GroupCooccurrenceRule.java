package com.puchain.fep.processor.validation.rule;

import java.util.List;
import java.util.Optional;

/**
 * 分组可选共现规则：一组字段必须同时有值或同时无值。
 *
 * <p>典型用途：报文规范 §2.1.3.1 分组字段"同时使用或同时不使用"——XSD 表达不了
 * 这种跨字段共现约束。"有值"按非空白文本判定（{@link RuleContext#has}）。</p>
 */
public final class GroupCooccurrenceRule implements ValidationRule {

    private final List<String> groupFields;

    /**
     * @param groupFields 分组字段 local-name 列表，至少 2 个
     * @throws IllegalArgumentException 分组少于 2 个字段
     */
    public GroupCooccurrenceRule(final List<String> groupFields) {
        if (groupFields == null || groupFields.size() < 2) {
            throw new IllegalArgumentException("分组共现规则至少需要 2 个字段");
        }
        this.groupFields = List.copyOf(groupFields);
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final List<String> present = groupFields.stream().filter(ctx::has).toList();
        if (present.isEmpty() || present.size() == groupFields.size()) {
            return Optional.empty();
        }
        final List<String> missing = groupFields.stream()
                .filter(f -> !ctx.has(f)).toList();
        return Optional.of("分组字段 " + groupFields + " 须同时使用或同时不使用；已填 "
                + present + " 缺失 " + missing);
    }
}
