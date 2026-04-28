package com.puchain.fep.web.system.controller;

import com.puchain.fep.common.domain.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Transport provider introspection endpoint (P1c T7 v1a).
 *
 * <p>Exposes the active {@code fep.transport.provider} value (defaulting to
 * {@code "mock"}) so the admin UI's {@code MockBadge} component can decide
 * whether to render the "TLQ Mock Mode" indicator near connectivity / login
 * actions.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/web/system")
@Tag(name = "Transport Provider",
        description = "P1c §5.7 expose active fep.transport.provider for frontend MockBadge")
public class TransportProviderController {

    /** The active transport provider — either {@code "mock"} or {@code "tongtech"}. */
    private final String provider;

    /**
     * Construct via constructor injection of the {@code fep.transport.provider}
     * property. Defaults to {@code "mock"} when the property is unset.
     *
     * @param provider the transport provider identifier
     */
    public TransportProviderController(
            @Value("${fep.transport.provider:mock}") final String provider) {
        this.provider = provider;
    }

    /**
     * Return the active transport provider.
     *
     * @return JSON payload {@code {"provider": "mock" | "tongtech"}}
     */
    @GetMapping("/transport-provider")
    @Operation(summary = "Get active transport provider",
            description = "Returns the configured fep.transport.provider value for frontend MockBadge gating")
    @ApiResponse(responseCode = "200", description = "Provider returned")
    public ApiResult<Map<String, String>> getProvider() {
        return ApiResult.success(Map.of("provider", provider));
    }
}
