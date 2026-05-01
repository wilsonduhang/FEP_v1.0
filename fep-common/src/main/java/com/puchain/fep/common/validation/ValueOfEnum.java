package com.puchain.fep.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验字符串值必须是指定枚举类型的有效常量名。
 *
 * <p>触发于请求 DTO 的 {@code String} 字段映射到后端枚举之前 — 比让
 * {@code Enum.valueOf} 抛 {@link IllegalArgumentException} 早一步抓到非法
 * 输入，并由 Bean Validation 统一返回 HTTP 400 响应。</p>
 *
 * <p>示例：
 * <pre>{@code
 * @ValueOfEnum(enumClass = RoleDirection.class, message = "direction 必须是 RoleDirection 合法常量")
 * private String direction;
 * }</pre>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValueOfEnumValidator.class)
public @interface ValueOfEnum {

    /**
     * 目标枚举类。
     *
     * @return 枚举 Class 字面量
     */
    Class<? extends Enum<?>> enumClass();

    /**
     * 校验失败消息。允许使用 {@code {enumClass}} 占位符渲染常量列表。
     *
     * @return 消息
     */
    String message() default "值必须是 {enumClass} 的合法常量";

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
