package com.puchain.fep.processor.validation.rule;

import java.util.Optional;

/**
 * 业务校验规则 SPI。对单条报文的字段视图求值，违规返回错误描述。
 *
 * <p>规则机制由 AI 编码，规则实例参数由领域专家按人行规范定义（mode C）。</p>
 */
@FunctionalInterface
public interface ValidationRule {

    /**
     * 对报文字段视图求值。
     *
     * @param ctx 报文字段视图，非空
     * @return 违规时返回错误描述；通过时 {@link Optional#empty()}
     */
    Optional<String> evaluate(RuleContext ctx);
}
