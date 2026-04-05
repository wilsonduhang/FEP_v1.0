package com.puchain.fep.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 密码复杂度校验注解。
 *
 * <p>规则（PRD v1.3 §5.1.3 + P6a.1 冻结决策 #1）:</p>
 * <ul>
 *   <li>长度 8-20（需配合 {@code @Size} 同时使用）</li>
 *   <li>至少包含大写字母 / 小写字母 / 数字 三类中的<strong>两类</strong></li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordComplexityValidator.class)
public @interface PasswordComplexity {

    /**
     * 校验失败消息。
     *
     * @return 消息
     */
    String message() default "密码必须包含大写字母/小写字母/数字中的至少两类";

    /**
     * 校验分组。
     *
     * @return 分组
     */
    Class<?>[] groups() default {};

    /**
     * 负载。
     *
     * @return 负载
     */
    Class<? extends Payload>[] payload() default {};
}
