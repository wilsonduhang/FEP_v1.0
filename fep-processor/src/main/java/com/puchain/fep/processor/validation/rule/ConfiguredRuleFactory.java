package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.converter.type.MessageType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Predicate;

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
            case "CONDITIONAL_REQUIRED" -> new ConditionalRequiredRule(
                    def.getField(), def.getTriggerField(), conditionalTrigger(def));
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

    /**
     * 构建 CONDITIONAL_REQUIRED 规则的触发谓词。
     *
     * <p>{@code triggerOperator} 为空时回落 Phase 1 legacy 语义（触发字段存在即要求目标）；
     * 否则由 {@link TriggerOperator} 按算子与值集构建方向感知谓词。</p>
     *
     * @param def 规则定义
     * @return 触发谓词
     * @throws IllegalArgumentException triggerOperator 非法或 triggerValues 个数不满足算子约束
     */
    private static Predicate<String> conditionalTrigger(
            final RuleDefinitionProperties.RuleDef def) {
        final String op = def.getTriggerOperator();
        if (op == null || op.isBlank()) {
            return v -> !v.isBlank();
        }
        return TriggerOperator.valueOf(op).toPredicate(def.getTriggerValues());
    }
}
