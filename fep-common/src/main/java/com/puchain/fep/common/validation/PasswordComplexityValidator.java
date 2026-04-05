package com.puchain.fep.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 密码复杂度校验器 — "三选二"字符类型要求。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class PasswordComplexityValidator implements ConstraintValidator<PasswordComplexity, String> {

    private static final int REQUIRED_CATEGORIES = 2;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // null/空由 @NotBlank 处理，此处放行
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        int categories = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0);
        return categories >= REQUIRED_CATEGORIES;
    }
}
