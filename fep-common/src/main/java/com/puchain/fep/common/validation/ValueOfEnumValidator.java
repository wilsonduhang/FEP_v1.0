package com.puchain.fep.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

/**
 * {@link ValueOfEnum} 校验器：值必须存在于目标枚举的常量名列表（区分大小写）。
 *
 * <p>{@code null} 与空串放行 — 由 {@code @NotBlank} 处理，避免双重报错。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ValueOfEnumValidator implements ConstraintValidator<ValueOfEnum, CharSequence> {

    private List<String> acceptedValues;

    @Override
    public void initialize(final ValueOfEnum annotation) {
        acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .toList();
    }

    @Override
    public boolean isValid(final CharSequence value, final ConstraintValidatorContext context) {
        if (value == null || value.length() == 0) {
            return true;
        }
        return acceptedValues.contains(value.toString());
    }
}
