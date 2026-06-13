package com.puchain.fep.processor.validation.rule;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 配置驱动的业务校验规则定义。键为报文 msgNo，值为该报文的规则列表。
 *
 * <p>规则内容由领域专家按人行规范在配置中维护（mode C），引擎启动期装配。</p>
 */
@ConfigurationProperties(prefix = "fep.validation")
public class RuleDefinitionProperties {

    /** msgNo → 规则定义列表。 */
    private Map<String, List<RuleDef>> rules = Map.of();

    /**
     * @return msgNo → 规则定义列表（默认空 Map）
     */
    public Map<String, List<RuleDef>> getRules() {
        return rules;
    }

    /**
     * @param rules msgNo → 规则定义列表
     */
    public void setRules(final Map<String, List<RuleDef>> rules) {
        this.rules = rules;
    }

    /** 单条规则定义。type ∈ {ENUM, CONDITIONAL_REQUIRED, CROSS_FIELD, DEPENDENT_ENUM, GROUP_COOCCURRENCE}。 */
    public static class RuleDef {
        private String type;
        private String field;
        private String triggerField;
        private String triggerOperator;
        private List<String> triggerValues = new ArrayList<>();
        private String compareField;
        private String operator;
        private List<String> allowed = new ArrayList<>();
        private String keyField;
        private Map<String, List<String>> allowedByKey = Map.of();
        private List<String> groupFields = new ArrayList<>();
        private String scope;

        /** @return 规则类型 */
        public String getType() {
            return type;
        }

        /** @param type 规则类型 */
        public void setType(final String type) {
            this.type = type;
        }

        /** @return 主字段 local-name */
        public String getField() {
            return field;
        }

        /** @param field 主字段 local-name */
        public void setField(final String field) {
            this.field = field;
        }

        /** @return 触发字段 local-name（CONDITIONAL_REQUIRED 用） */
        public String getTriggerField() {
            return triggerField;
        }

        /** @param triggerField 触发字段 local-name */
        public void setTriggerField(final String triggerField) {
            this.triggerField = triggerField;
        }

        /** @return 触发算子（CONDITIONAL_REQUIRED 用，对应 {@link TriggerOperator}；null 时回落 legacy 存在性判定） */
        public String getTriggerOperator() {
            return triggerOperator;
        }

        /** @param triggerOperator 触发算子 */
        public void setTriggerOperator(final String triggerOperator) {
            this.triggerOperator = triggerOperator;
        }

        /** @return 触发值集（CONDITIONAL_REQUIRED 用；EQUALS/NOT_EQUALS 须 1 个，IN/NOT_IN 须 ≥1 个） */
        public List<String> getTriggerValues() {
            return triggerValues;
        }

        /** @param triggerValues 触发值集 */
        public void setTriggerValues(final List<String> triggerValues) {
            this.triggerValues = triggerValues;
        }

        /** @return 比较字段 local-name（CROSS_FIELD 用） */
        public String getCompareField() {
            return compareField;
        }

        /** @param compareField 比较字段 local-name */
        public void setCompareField(final String compareField) {
            this.compareField = compareField;
        }

        /** @return 比较算子（CROSS_FIELD 用，对应 {@link CrossFieldComparisonRule.Operator}） */
        public String getOperator() {
            return operator;
        }

        /** @param operator 比较算子 */
        public void setOperator(final String operator) {
            this.operator = operator;
        }

        /** @return 允许值集合（ENUM 用） */
        public List<String> getAllowed() {
            return allowed;
        }

        /** @param allowed 允许值集合 */
        public void setAllowed(final List<String> allowed) {
            this.allowed = allowed;
        }

        /** @return key 字段 local-name（DEPENDENT_ENUM 用） */
        public String getKeyField() {
            return keyField;
        }

        /** @param keyField key 字段 local-name */
        public void setKeyField(final String keyField) {
            this.keyField = keyField;
        }

        /** @return key 值 → 目标允许集（DEPENDENT_ENUM 用） */
        public Map<String, List<String>> getAllowedByKey() {
            return allowedByKey;
        }

        /** @param allowedByKey key 值 → 目标允许集 */
        public void setAllowedByKey(final Map<String, List<String>> allowedByKey) {
            this.allowedByKey = allowedByKey;
        }

        /** @return 分组共现字段列表（GROUP_COOCCURRENCE 用） */
        public List<String> getGroupFields() {
            return groupFields;
        }

        /** @param groupFields 分组共现字段列表 */
        public void setGroupFields(final List<String> groupFields) {
            this.groupFields = groupFields;
        }

        /** @return 分组探测作用域（GROUP_COOCCURRENCE 用，MESSAGE/HEAD，缺省 MESSAGE） */
        public String getScope() {
            return scope;
        }

        /** @param scope 分组探测作用域 */
        public void setScope(final String scope) {
            this.scope = scope;
        }
    }
}
