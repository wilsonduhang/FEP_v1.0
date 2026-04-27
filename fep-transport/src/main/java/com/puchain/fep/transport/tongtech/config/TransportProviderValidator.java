package com.puchain.fep.transport.tongtech.config;

import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Provider configuration startup validator.
 *
 * <p>Runs in {@link PostConstruct @PostConstruct} before downstream beans are wired and throws
 * {@link IllegalStateException} when the application is misconfigured. Two failure modes:
 * <ul>
 *   <li>An unrecognised {@code fep.transport.provider} value (anything outside
 *       {@code mock|tongtech}).</li>
 *   <li>A production profile is active but the provider was not explicitly set to
 *       {@code tongtech} — this catches the case where {@code matchIfMissing=true}
 *       silently falls back to {@code mock} in production.</li>
 * </ul></p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class TransportProviderValidator {

    private static final Set<String> ALLOWED = Set.of("mock", "tongtech");
    private static final Set<String> PROD_PROFILES = Set.of("prod", "production");

    private final String provider;
    private final Environment environment;

    /**
     * Construct the validator with the resolved provider value and Spring environment.
     *
     * @param provider the value of {@code fep.transport.provider} (defaults to {@code mock})
     * @param environment the Spring {@link Environment} used to inspect active profiles
     */
    public TransportProviderValidator(
            @Value("${fep.transport.provider:mock}") final String provider,
            final Environment environment) {
        this.provider = provider;
        this.environment = environment;
    }

    /**
     * Validate the provider value and active profile combination.
     *
     * @throws IllegalStateException if the provider is unrecognised, or if a production
     *     profile is active without {@code provider=tongtech}
     */
    @PostConstruct
    void validate() {
        if (!ALLOWED.contains(provider)) {
            throw new IllegalStateException(
                "Invalid fep.transport.provider value: '" + provider
                + "', expected mock|tongtech");
        }

        for (String activeProfile : environment.getActiveProfiles()) {
            if (PROD_PROFILES.contains(activeProfile) && !"tongtech".equals(provider)) {
                throw new IllegalStateException(
                    "Production profile (" + activeProfile + ") requires "
                    + "fep.transport.provider=tongtech, got: '" + provider + "'");
            }
        }
    }
}
