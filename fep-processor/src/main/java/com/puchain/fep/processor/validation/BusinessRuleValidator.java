package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import com.puchain.fep.processor.validation.rule.RuleContext;
import com.puchain.fep.processor.validation.rule.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务语义/跨字段校验器。在 XSD 结构校验通过后运行，按报文类型加载注册规则逐条求值，
 * 聚合全部违规为 {@link ValidationResult}（收集式，不短路）。
 *
 * <p>无注册规则时默认放行（{@link ValidationResult#ok()}），不阻断尚未配置规则的报文类型。</p>
 */
@Component
public class BusinessRuleValidator {

    private final MessageRuleRegistry registry;

    /**
     * @param registry 规则注册表，非空
     */
    public BusinessRuleValidator(final MessageRuleRegistry registry) {
        this.registry = registry;
    }

    /**
     * 对单条报文执行业务规则校验。
     *
     * @param type 报文类型，非空
     * @param xml  UTF-8 报文字节，非空
     * @return 校验结果；全部规则通过或无规则时 valid=true
     * @throws ValidationException XML 解析失败
     */
    public ValidationResult validate(final MessageType type, final byte[] xml) {
        final List<ValidationRule> applicable = registry.rulesFor(type);
        if (applicable.isEmpty()) {
            return ValidationResult.ok();
        }
        final RuleContext ctx = RuleContext.parse(xml);
        final List<String> violations = new ArrayList<>();
        for (final ValidationRule rule : applicable) {
            rule.evaluate(ctx).ifPresent(violations::add);
        }
        return violations.isEmpty() ? ValidationResult.ok() : ValidationResult.failed(violations);
    }
}
