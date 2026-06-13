package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.converter.type.MessageType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 将配置规则定义装配为 {@link ValidationRule} 并注册到 {@link MessageRuleRegistry}（启动期一次）。
 *
 * <p>报文号支持 {@code "*"} 通配（yaml 键须写 {@code "[*]"} 防 relaxed binding 剥字符）：
 * 该键下规则注册到全部 {@link MessageType}，用于全报文通用约束（如报文头分组）。</p>
 */
@Component
public class ConfiguredRuleFactory {

    /** 通配报文号键（yaml 中写 "[*]"）。 */
    private static final String WILDCARD_MSG_NO = "*";

    private final RuleDefinitionProperties properties;
    private final MessageRuleRegistry registry;

    /**
     * @param properties 配置驱动规则定义，非空
     * @param registry   规则注册表，非空
     */
    public ConfiguredRuleFactory(final RuleDefinitionProperties properties,
                                 final MessageRuleRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    /**
     * 启动期把配置规则注册到注册表。
     *
     * @throws IllegalArgumentException 配置中存在未知报文号
     */
    @PostConstruct
    public void registerConfiguredRules() {
        properties.getRules().forEach((msgNo, defs) -> {
            if (WILDCARD_MSG_NO.equals(msgNo)) {
                defs.forEach(def -> {
                    final ValidationRule rule = build(def);
                    for (final MessageType t : MessageType.values()) {
                        registry.register(t, rule);
                    }
                });
                return;
            }
            final MessageType type = MessageType.byMsgNo(msgNo)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "未知报文号配置 fep.validation.rules: " + msgNo));
            defs.forEach(def -> registry.register(type, build(def)));
        });
    }

    /**
     * 按规则定义构建规则实例。
     *
     * @param def 规则定义，非空
     * @return 规则实例
     * @throws IllegalArgumentException type 不识别
     */
    public static ValidationRule build(final RuleDefinitionProperties.RuleDef def) {
        return switch (def.getType()) {
            case "ENUM" -> new EnumMembershipRule(def.getField(), Set.copyOf(def.getAllowed()));
            // Phase 1：触发谓词简化为"触发字段非空即要求目标字段"；完整谓词表达式 DSL 留 Phase 2。
            case "CONDITIONAL_REQUIRED" -> new ConditionalRequiredRule(
                    def.getField(), def.getTriggerField(), v -> !v.isBlank());
            case "CROSS_FIELD" -> new CrossFieldComparisonRule(
                    def.getField(), def.getCompareField(),
                    CrossFieldComparisonRule.Operator.valueOf(def.getOperator()));
            case "DEPENDENT_ENUM" -> new DependentEnumRule(
                    def.getField(), def.getKeyField(), def.getAllowedByKey());
            case "GROUP_COOCCURRENCE" -> new GroupCooccurrenceRule(def.getGroupFields(),
                    def.getScope() == null ? GroupCooccurrenceRule.Scope.MESSAGE
                            : GroupCooccurrenceRule.Scope.valueOf(def.getScope()));
            default -> throw new IllegalArgumentException("未知规则类型: " + def.getType());
        };
    }
}
