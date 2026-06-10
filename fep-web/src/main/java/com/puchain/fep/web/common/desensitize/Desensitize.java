package com.puchain.fep.web.common.desensitize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DTO 字段声明式脱敏 — Jackson 序列化时按 {@link #value()} 类型调
 * {@link com.puchain.fep.security.api.DesensitizeService} 脱敏。
 *
 * <p>用法：{@code @Desensitize(DesensitizeType.PHONE) private String phone;}。
 * 参照既有 {@code @JsonSerialize(using=ToStringSerializer.class)} 范式。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeJsonSerializer.class)
public @interface Desensitize {

    /**
     * 脱敏类型。
     *
     * @return 类型
     */
    DesensitizeType value();
}
