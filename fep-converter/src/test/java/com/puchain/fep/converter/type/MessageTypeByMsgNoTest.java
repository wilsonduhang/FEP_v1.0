package com.puchain.fep.converter.type;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTypeByMsgNoTest {

    @Test
    void byMsgNo_shouldResolveAll44Messages() {
        for (MessageType type : MessageType.values()) {
            Optional<MessageType> resolved = MessageType.byMsgNo(type.msgNo());
            assertThat(resolved).as("msgNo=%s", type.msgNo()).contains(type);
        }
    }

    @Test
    void byMsgNo_shouldReturnEmpty_forNull() {
        assertThat(MessageType.byMsgNo(null)).isEmpty();
    }

    @Test
    void byMsgNo_shouldReturnEmpty_forUnknownCode() {
        assertThat(MessageType.byMsgNo("9999")).isEmpty();
        assertThat(MessageType.byMsgNo("")).isEmpty();
    }

    @Test
    void byMsgNo_shouldResolveRealtimeMessages() {
        assertThat(MessageType.byMsgNo("1001")).contains(MessageType.MSG_1001);
        assertThat(MessageType.byMsgNo("2001")).contains(MessageType.MSG_2001);
        assertThat(MessageType.byMsgNo("1004")).contains(MessageType.MSG_1004);
        assertThat(MessageType.byMsgNo("2004")).contains(MessageType.MSG_2004);
        assertThat(MessageType.byMsgNo("9005")).contains(MessageType.MSG_9005);
    }
}
