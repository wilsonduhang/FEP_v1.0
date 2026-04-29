package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.transport.tongtech.config.TongtechTlqProperties;
import com.puchain.fep.transport.tongtech.error.TongtechErrorMapper;
import com.tongtech.tlq.base.TlqConnection;
import com.tongtech.tlq.base.TlqException;
import com.tongtech.tlq.base.TlqQCU;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TongtechTlqConnectionFactory}.
 *
 * <p>Uses {@link Mockito#mockConstruction(Class)} to intercept the
 * {@code new TlqConnection(...)} call inside {@link TongtechTlqConnectionFactory#connect()}
 * without needing a real TLQ broker. Three cases lock the contract:</p>
 * <ol>
 *   <li>Successful connect → openQCU invoked, {@code isConnected()} returns true.</li>
 *   <li>SDK constructor throws → wrapped as {@link FepBusinessException}
 *       with {@link FepErrorCode#TRANS_7002}.</li>
 *   <li>{@code disconnect()} is idempotent — second call is a no-op.</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechTlqConnectionFactoryTest {

    private TongtechTlqProperties props() {
        TongtechTlqProperties p = new TongtechTlqProperties();
        p.setBrokerHost("127.0.0.1");
        p.setBrokerPort(10024);
        p.setBrokerId(1);
        p.setUserName("user");
        p.setPassword("pwd");
        p.setQcuName("QCU1");
        p.setConnTimeSec(30);
        p.setReplyTmoutSec(30);
        p.setSecExitFlag(0);
        return p;
    }

    @Test
    @DisplayName("connect: success → TlqConnection constructed + QCU opened + isConnected=true")
    void connect_success_shouldOpenConnectionAndQCU() throws Exception {
        TlqQCU mockQcu = Mockito.mock(TlqQCU.class);
        TongtechErrorMapper mapper = new TongtechErrorMapper();

        try (MockedConstruction<TlqConnection> mc = Mockito.mockConstruction(
                TlqConnection.class,
                (mock, context) -> Mockito.when(mock.openQCU("QCU1")).thenReturn(mockQcu))) {

            TongtechTlqConnectionFactory factory = new TongtechTlqConnectionFactory(props(), mapper);
            assertThat(factory.isConnected()).isFalse();

            factory.connect();

            assertThat(factory.isConnected()).isTrue();
            assertThat(mc.constructed()).hasSize(1);
            TlqConnection mockConn = mc.constructed().get(0);
            Mockito.verify(mockConn).openQCU("QCU1");
        }
    }

    @Test
    @DisplayName("connect: SDK TlqException → FepBusinessException(TRANS_7002), state remains disconnected")
    void connect_whenSdkThrows_shouldRaiseTrans7002() {
        TongtechErrorMapper mapper = new TongtechErrorMapper();

        // Simulate broker rejection via openQCU throwing (Mockito's mockConstruction
        // executes the initializer after the mock instance is built, so the
        // constructor itself cannot be made to throw — making openQCU throw
        // exercises the same {@code catch (TlqException)} branch in connect()).
        try (MockedConstruction<TlqConnection> mc = Mockito.mockConstruction(
                TlqConnection.class,
                (mock, context) -> Mockito.when(mock.openQCU("QCU1"))
                        .thenThrow(new TlqException("connect refused by broker")))) {

            TongtechTlqConnectionFactory factory = new TongtechTlqConnectionFactory(props(), mapper);

            assertThatThrownBy(factory::connect)
                    .isInstanceOf(FepBusinessException.class)
                    .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                            .isEqualTo(FepErrorCode.TRANS_7002));
            assertThat(factory.isConnected()).isFalse();
        }
    }

    @Test
    @DisplayName("disconnect: idempotent — second call no-op, qcu.close()/conn.close() invoked exactly once")
    void disconnect_isIdempotent() throws Exception {
        TlqQCU mockQcu = Mockito.mock(TlqQCU.class);
        TongtechErrorMapper mapper = new TongtechErrorMapper();

        try (MockedConstruction<TlqConnection> mc = Mockito.mockConstruction(
                TlqConnection.class,
                (mock, context) -> Mockito.when(mock.openQCU("QCU1")).thenReturn(mockQcu))) {

            TongtechTlqConnectionFactory factory = new TongtechTlqConnectionFactory(props(), mapper);
            factory.connect();
            assertThat(factory.isConnected()).isTrue();

            factory.disconnect();
            assertThat(factory.isConnected()).isFalse();

            // Second disconnect must not throw and must not invoke close again.
            factory.disconnect();
            assertThat(factory.isConnected()).isFalse();

            // qcu.close() called exactly once across two disconnects (idempotent).
            Mockito.verify(mockQcu, Mockito.times(1)).close();
            TlqConnection mockConn = mc.constructed().get(0);
            Mockito.verify(mockConn, Mockito.times(1)).close();
        }
    }
}
