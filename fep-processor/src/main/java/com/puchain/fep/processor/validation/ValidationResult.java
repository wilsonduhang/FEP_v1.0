package com.puchain.fep.processor.validation;

import java.util.List;

/**
 * XSD 校验结果。{@code errors} 为空当且仅当 {@code valid=true}。
 *
 * <p>The {@code errors} list is defensively copied in the canonical constructor
 * and re-wrapped as unmodifiable in the accessor to prevent external mutation
 * (SpotBugs EI_EXPOSE_REP / EI_EXPOSE_REP2).</p>
 *
 * @param valid  是否通过校验
 * @param errors 错误详情列表（每条包含行号、列号、SAX 消息）
 */
public record ValidationResult(boolean valid, List<String> errors) {

    /**
     * 压缩规范构造器：对 errors 做防御性不可变复制，避免外部通过原引用篡改。
     *
     * @param valid  校验是否通过
     * @param errors 错误详情列表（不能为 null）
     */
    public ValidationResult {
        errors = List.copyOf(errors);
    }

    /**
     * 获取错误列表（不可修改视图）。
     *
     * @return 不可修改的错误详情列表
     */
    @Override
    public List<String> errors() {
        return errors;
    }

    /**
     * 返回成功校验结果。
     *
     * @return 合法且 errors 为空的 ValidationResult
     */
    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    /**
     * 返回失败校验结果，errors 被不可变复制。
     *
     * @param errors 错误详情列表（不能为 null）
     * @return 不合法且 errors 不为空的 ValidationResult
     */
    public static ValidationResult failed(final List<String> errors) {
        return new ValidationResult(false, errors);
    }

    /**
     * 首条错误详情；无错误时返回 {@code "unknown"}（供流水线日志/失败原因统一取值）。
     *
     * @return 首条错误或 "unknown"
     */
    public String firstError() {
        return errors.isEmpty() ? "unknown" : errors.get(0);
    }
}
