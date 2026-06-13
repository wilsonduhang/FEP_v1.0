package com.puchain.fep.processor.validation.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 依赖枚举规则：目标字段的合法枚举集由 key 字段的取值决定。
 *
 * <p>典型用途：业务小类（SecondClass）合法集随业务大类（MainClass）变——报文规范 §5.1.3，
 * XSD 无法表达此依赖关系。</p>
 *
 * <p>边界语义：key 缺失 / 目标缺失 / key 未在映射表中（如自由定义的 GENERAL 大类）
 * 均不违规；key 已映射且目标不在该集合时违规。key 自身合法性由独立 {@link EnumMembershipRule} 把关。</p>
 *
 * <p>同名字段重复出现（批量报文嵌套明细项）时按文档顺序成对 (key[i], value[i]) 逐对校验；
 * 两侧出现次数不等时仅校验共同前缀（超出部分由 XSD minOccurs 兜底，不误报）。</p>
 */
public final class DependentEnumRule implements ValidationRule {

    private final String field;
    private final String keyField;
    private final Map<String, Set<String>> allowedByKey;

    /**
     * @param field        目标字段 local-name
     * @param keyField     决定合法集的 key 字段 local-name
     * @param allowedByKey key 值 → 目标字段允许集；未含的 key 视为不约束
     * @throws NullPointerException 若 allowedByKey 为 null 或其任一 value 为 null
     */
    public DependentEnumRule(final String field,
                             final String keyField,
                             final Map<String, List<String>> allowedByKey) {
        this.field = field;
        this.keyField = keyField;
        final Map<String, Set<String>> copy = new LinkedHashMap<>();
        allowedByKey.forEach((k, v) -> copy.put(k, Set.copyOf(v)));
        this.allowedByKey = Map.copyOf(copy);
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final List<String> keys = ctx.values(keyField);
        final List<String> vals = ctx.values(field);
        // 按文档顺序成对 (key[i], value[i])；次数不等时只校验共同前缀，超出部分由 XSD minOccurs 兜底
        final int n = Math.min(keys.size(), vals.size());
        final List<String> violations = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final Set<String> allowed = allowedByKey.get(keys.get(i));
            if (allowed != null && !allowed.contains(vals.get(i))) {
                violations.add("字段 " + field + " 值 [" + vals.get(i) + "] 在 "
                        + keyField + "=[" + keys.get(i) + "] 下不在允许集合 " + allowed);
            }
        }
        return violations.isEmpty() ? Optional.empty()
                : Optional.of(String.join("; ", violations));
    }
}
