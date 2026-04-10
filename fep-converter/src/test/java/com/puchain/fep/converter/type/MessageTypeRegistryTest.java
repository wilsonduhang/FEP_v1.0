package com.puchain.fep.converter.type;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTypeRegistryTest {

    private final MessageTypeRegistry registry = new MessageTypeRegistry();

    @Test
    void lookup_shouldReturnTypeForKnownMsgNo() {
        assertThat(registry.lookup("1001")).isEqualTo(MessageType.MSG_1001);
        assertThat(registry.lookup("3101")).isEqualTo(MessageType.MSG_3101);
    }

    @Test
    void lookup_unknownMsgNo_shouldRaiseConv8003() {
        assertThatThrownBy(() -> registry.lookup("0000"))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8003))
                .hasMessageContaining("0000");
    }

    @Test
    void isRegistered_shouldDiscriminateKnownAndUnknown() {
        assertThat(registry.isRegistered("9120")).isTrue();
        assertThat(registry.isRegistered("9999")).isFalse();
    }

    @Test
    void size_shouldReturn44() {
        assertThat(registry.size()).isEqualTo(44);
    }
}
