package com.puchain.fep.transport.tongtech.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link TransportProviderValidator}.
 *
 * <p>Covers the four startup scenarios required by P1c Task 2 v1a Step 4:
 * <ol>
 *   <li>valid provider (both {@code mock} and {@code tongtech}) succeeds without active profiles;</li>
 *   <li>unrecognised provider value triggers an {@link IllegalStateException};</li>
 *   <li>production profile combined with {@code provider=mock} (matchIfMissing fallback) fails;</li>
 *   <li>production profile combined with {@code provider=tongtech} succeeds.</li>
 * </ol></p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TransportProviderValidatorTest {

    @Test
    @DisplayName("validate() accepts mock and tongtech with no active profile")
    void validProvider_shouldNotThrow() {
        MockEnvironment env = new MockEnvironment();

        TransportProviderValidator mockValidator = new TransportProviderValidator("mock", env);
        TransportProviderValidator tongtechValidator =
            new TransportProviderValidator("tongtech", env);

        assertThatCode(mockValidator::validate).doesNotThrowAnyException();
        assertThatCode(tongtechValidator::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate() throws IllegalStateException for unknown provider value")
    void invalidProvider_shouldThrow() {
        MockEnvironment env = new MockEnvironment();
        TransportProviderValidator validator = new TransportProviderValidator("foo", env);

        assertThatThrownBy(validator::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("foo")
            .hasMessageContaining("expected mock|tongtech");
    }

    @Test
    @DisplayName("validate() rejects prod profile when provider is not tongtech")
    void prodProfile_withoutTongtech_shouldThrow() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        TransportProviderValidator validator = new TransportProviderValidator("mock", env);

        assertThatThrownBy(validator::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Production profile")
            .hasMessageContaining("requires fep.transport.provider=tongtech")
            .hasMessageContaining("'mock'");
    }

    @Test
    @DisplayName("validate() accepts prod profile when provider is tongtech")
    void prodProfile_withTongtech_shouldPass() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        TransportProviderValidator validator = new TransportProviderValidator("tongtech", env);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
