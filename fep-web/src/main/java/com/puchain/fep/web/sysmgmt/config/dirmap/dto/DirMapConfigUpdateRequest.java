package com.puchain.fep.web.sysmgmt.config.dirmap.dto;

import com.puchain.fep.common.validation.ValueOfEnum;
import com.puchain.fep.processor.routing.ProcessingMode;
import com.puchain.fep.processor.routing.RoleDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DIR-MAP config edit request body.
 *
 * <p>Bound by {@code PUT /api/v1/sys/config/dir-map/{messageType}/{accessRole}}.
 * Path variables identify the target row; this body carries the new values.
 *
 * <p>{@code direction} 与 {@code processingMode} 在 Bean Validation 阶段就被
 * {@link ValueOfEnum} 拦截，非法常量名直接 400 + Bean Validation 标准错误体；
 * 不再走 Service 层 {@code Enum.valueOf} 抛 {@link IllegalArgumentException}
 * 的路径，UX 更早更清晰。
 *
 * @param direction      new {@link RoleDirection} 常量名
 * @param requiresFep    new requires-FEP flag
 * @param processingMode new {@link ProcessingMode} 常量名
 * @param changeReason   optional change rationale, persisted to history
 * @author FEP Team
 * @since 1.0.0
 */
public record DirMapConfigUpdateRequest(
        @NotBlank
        @ValueOfEnum(enumClass = RoleDirection.class,
                message = "direction 必须是 RoleDirection 合法常量")
        String direction,

        @NotNull Boolean requiresFep,

        @NotBlank
        @ValueOfEnum(enumClass = ProcessingMode.class,
                message = "processingMode 必须是 ProcessingMode 合法常量")
        String processingMode,

        String changeReason) { }
