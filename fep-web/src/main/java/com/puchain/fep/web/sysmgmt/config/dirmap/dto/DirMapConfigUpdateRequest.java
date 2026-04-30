package com.puchain.fep.web.sysmgmt.config.dirmap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DIR-MAP config edit request body.
 *
 * <p>Bound by {@code PUT /api/v1/sys/config/dir-map/{messageType}/{accessRole}}.
 * Path variables identify the target row; this body carries the new values.
 *
 * @param direction      new {@link com.puchain.fep.processor.routing.RoleDirection} name
 * @param requiresFep    new requires-FEP flag
 * @param processingMode new {@link com.puchain.fep.processor.routing.ProcessingMode} name
 * @param changeReason   optional change rationale, persisted to history
 * @author FEP Team
 * @since 1.0.0
 */
public record DirMapConfigUpdateRequest(
        @NotBlank String direction,
        @NotNull Boolean requiresFep,
        @NotBlank String processingMode,
        String changeReason) { }
