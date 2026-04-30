package com.puchain.fep.web.sysmgmt.config.dirmap.dto;

import java.time.Instant;

/**
 * DIR-MAP history audit row DTO.
 *
 * <p>Returned by {@code GET /api/v1/sys/config/dir-map/{messageType}/{accessRole}/history}.
 * Captures the complete before/after snapshot for a single edit, plus actor
 * and timestamp metadata.
 *
 * @param historyId       UUID32 primary key
 * @param oldDirection    pre-edit {@link com.puchain.fep.processor.routing.RoleDirection} name
 * @param oldRequiresFep  pre-edit requires-FEP flag (boxed: history rows pre-V20 may be null)
 * @param oldMode         pre-edit {@link com.puchain.fep.processor.routing.ProcessingMode} name
 * @param newDirection    post-edit direction name
 * @param newRequiresFep  post-edit requires-FEP flag
 * @param newMode         post-edit mode name
 * @param changedBy       editor username
 * @param changedAt       edit timestamp
 * @param changeReason    optional rationale supplied by editor
 * @author FEP Team
 * @since 1.0.0
 */
public record DirMapHistoryResponse(
        String historyId, String oldDirection, Boolean oldRequiresFep, String oldMode,
        String newDirection, boolean newRequiresFep, String newMode,
        String changedBy, Instant changedAt, String changeReason) { }
