package com.puchain.fep.transport.tongtech.error;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.tongtech.tlq.base.TlqException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TongtechErrorMapper}.
 *
 * <p>v1a strategy: cause-keyword-driven mapping (no errno hardcoding) until
 * P1c-IT-bridge real-machine validation calibrates the SDK errno table
 * (Plan §Risk Register R8/R9). Each branch of {@link TongtechErrorMapper#mapCause}
 * gets a dedicated test case to lock the keyword → {@link FepErrorCode} mapping.</p>
 *
 * <p>Note: {@code TlqException} is a {@code final class}; Mockito 5.x
 * (bundled with Spring Boot 3.x) supports inline mocking of final classes
 * by default — no {@code mockito-extensions} configuration required.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechErrorMapperTest {

    private final TongtechErrorMapper mapper = new TongtechErrorMapper();

    @Test
    @DisplayName("connection cause keyword → TRANS_7002 with cause preserved")
    void connectionCause_shouldMapToTrans7002() {
        TlqException ex = Mockito.mock(TlqException.class);
        Mockito.when(ex.getErrorCause()).thenReturn("connect to broker refused");
        Mockito.when(ex.getTlqErrno()).thenReturn(0);
        Mockito.when(ex.getErrorCode()).thenReturn("TLQ-1001");

        FepBusinessException result = mapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(FepErrorCode.TRANS_7002);
        assertThat(result.getCause()).isSameAs(ex);
    }

    @Test
    @DisplayName("receive-timeout cause keyword → TRANS_7005")
    void receiveTimeoutCause_shouldMapToTrans7005() {
        TlqException ex = Mockito.mock(TlqException.class);
        Mockito.when(ex.getErrorCause()).thenReturn("receive timeout while polling queue");

        FepBusinessException result = mapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(FepErrorCode.TRANS_7005);
    }

    @Test
    @DisplayName("queue-not-found cause keyword → TRANS_7008")
    void queueNotFoundCause_shouldMapToTrans7008() {
        TlqException ex = Mockito.mock(TlqException.class);
        Mockito.when(ex.getErrorCause()).thenReturn("queue not found: FEP_REALTIME_REQ");

        FepBusinessException result = mapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(FepErrorCode.TRANS_7008);
    }

    @Test
    @DisplayName("ack-failure cause keyword → TRANS_7007")
    void ackCause_shouldMapToTrans7007() {
        TlqException ex = Mockito.mock(TlqException.class);
        Mockito.when(ex.getErrorCause()).thenReturn("ack failure: message id not found");

        FepBusinessException result = mapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(FepErrorCode.TRANS_7007);
    }

    @Test
    @DisplayName("admin-rejected cause keyword → TRANS_7006")
    void adminCause_shouldMapToTrans7006() {
        TlqException ex = Mockito.mock(TlqException.class);
        Mockito.when(ex.getErrorCause()).thenReturn("admin operation rejected by broker");

        FepBusinessException result = mapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(FepErrorCode.TRANS_7006);
    }

    @Test
    @DisplayName("oversized-payload cause keyword → TRANS_7001")
    void oversizedCause_shouldMapToTrans7001() {
        TlqException ex = Mockito.mock(TlqException.class);
        Mockito.when(ex.getErrorCause()).thenReturn("message oversized: payload exceeds 24KB");

        FepBusinessException result = mapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(FepErrorCode.TRANS_7001);
    }

    @Test
    @DisplayName("unknown cause keyword → TRANS_7003 fallback with cause preserved")
    void unknownCause_shouldFallbackToTrans7003() {
        TlqException ex = Mockito.mock(TlqException.class);
        // Cause string deliberately avoids any keyword from mapCause() to exercise fallback.
        Mockito.when(ex.getErrorCause()).thenReturn("xyz unrecognized fault flag 99");
        Mockito.when(ex.getTlqErrno()).thenReturn(99999);
        Mockito.when(ex.getErrorCode()).thenReturn("TLQ-9999");

        FepBusinessException result = mapper.mapException(ex);

        assertThat(result.getErrorCode()).isEqualTo(FepErrorCode.TRANS_7003);
        assertThat(result.getCause()).isSameAs(ex);
    }
}
