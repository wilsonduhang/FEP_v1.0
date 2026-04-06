package com.puchain.fep.web.sysmgmt.log.annotation;

import com.puchain.fep.web.sysmgmt.log.domain.OperationType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解，标记在 Controller 方法上，由 AOP 切面自动记录操作审计日志。
 *
 * <p>参见 PRD v1.3 §8.3 操作审计日志全覆盖。使用示例：</p>
 * <pre>
 * {@code
 * @OperationLog(module = "系统管理-用户", type = OperationType.CREATE, description = "新建用户")
 * @PostMapping
 * public ApiResult<Void> createUser(@RequestBody SysUserCreateRequest req) { ... }
 * }
 * </pre>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 功能模块名称，如"系统管理-用户"。
     *
     * @return 模块名称
     */
    String module();

    /**
     * 操作类型。
     *
     * @return 操作类型枚举
     */
    OperationType type();

    /**
     * 操作描述，可为空。
     *
     * @return 操作描述
     */
    String description() default "";
}
