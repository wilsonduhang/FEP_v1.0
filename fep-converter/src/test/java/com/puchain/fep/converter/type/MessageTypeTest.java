package com.puchain.fep.converter.type;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MessageTypeTest {

    @Test
    void shouldContainExactly44Types() {
        assertThat(MessageType.values()).hasSize(44);
    }

    @Test
    void realtimeCategoryShouldHave4Types() {
        long count = Arrays.stream(MessageType.values())
                .filter(t -> t.category() == MessageCategory.REALTIME).count();
        assertThat(count).isEqualTo(4);
    }

    @Test
    void batchCategoryShouldHave8Types() {
        long count = Arrays.stream(MessageType.values())
                .filter(t -> t.category() == MessageCategory.BATCH).count();
        assertThat(count).isEqualTo(8);
    }

    @Test
    void supplyChainCategoryShouldHave23Types() {
        long count = Arrays.stream(MessageType.values())
                .filter(t -> t.category() == MessageCategory.SUPPLY_CHAIN).count();
        assertThat(count).isEqualTo(23);
    }

    @Test
    void commonCategoryShouldHave9Types() {
        long count = Arrays.stream(MessageType.values())
                .filter(t -> t.category() == MessageCategory.COMMON).count();
        assertThat(count).isEqualTo(9);
    }

    @Test
    void msg1001ShouldPairWith2001() {
        assertThat(MessageType.MSG_1001.responseMsgNo()).contains("2001");
    }

    @Test
    void msg3101ShouldHaveNoResponse() {
        assertThat(MessageType.MSG_3101.responseMsgNo()).isEmpty();
    }

    @Test
    void msg1101ShouldPairWith9120() {
        assertThat(MessageType.MSG_1101.responseMsgNo()).contains("9120");
    }

    @Test
    void msg9006ShouldPairWith9007() {
        assertThat(MessageType.MSG_9006.responseMsgNo()).contains("9007");
    }

    @Test
    void allMsgNoShouldBeFourDigits() {
        for (MessageType t : MessageType.values()) {
            assertThat(t.msgNo()).as("msgNo of " + t.name()).matches("\\d{4}");
        }
    }
}
