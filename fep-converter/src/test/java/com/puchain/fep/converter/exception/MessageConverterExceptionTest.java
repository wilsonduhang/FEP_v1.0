package com.puchain.fep.converter.exception;

import com.puchain.fep.common.domain.FepErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MessageConverterException} 单元测试。
 */
class MessageConverterExceptionTest {

    @Test
    void shouldCarryErrorCodeAndFormatMessage() {
        MessageConverterException ex =
                new MessageConverterException(FepErrorCode.CONV_8001, "test detail");
        assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8001);
        assertThat(ex.getMessage()).isEqualTo("CONV_8001: test detail");
    }

    @Test
    void shouldCarryCauseWhenProvided() {
        Throwable cause = new RuntimeException("root");
        MessageConverterException ex =
                new MessageConverterException(FepErrorCode.CONV_8002, "wrap", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void nullErrorCode_shouldRaiseNpe() {
        assertThatThrownBy(() ->
                new MessageConverterException(null, "no code"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("errorCode");
    }
}
