package com.puchain.fep.web.sysmgmt.config.dirmap.dto;

import java.time.Instant;

/**
 * DIR-MAP config row read DTO.
 *
 * <p>Returned by {@code GET /api/v1/sys/config/dir-map}. Eight fields capture
 * one row in {@code t_dir_map_config} plus the message display name resolved
 * from {@link com.puchain.fep.converter.type.MessageType}.
 *
 * @param messageType    message type code (e.g. {@code "3001"})
 * @param messageName    Chinese display name (e.g. {@code "业务进展查询"})
 * @param accessRole     {@link com.puchain.fep.processor.routing.AccessRole} name
 * @param direction      {@link com.puchain.fep.processor.routing.RoleDirection} name
 * @param requiresFep    whether FEP must process this row
 * @param processingMode {@link com.puchain.fep.processor.routing.ProcessingMode} name
 * @param updatedBy      last editor username
 * @param updatedAt      last edit timestamp
 * @author FEP Team
 * @since 1.0.0
 */
public record DirMapConfigResponse(
        String messageType, String messageName,
        String accessRole, String direction,
        boolean requiresFep, String processingMode,
        String updatedBy, Instant updatedAt) { }
