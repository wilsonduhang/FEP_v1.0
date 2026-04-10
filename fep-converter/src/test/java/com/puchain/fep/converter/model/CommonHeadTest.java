package com.puchain.fep.converter.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommonHeadTest {

    @Test
    void defaultVersionAndApp_shouldBeSet() {
        CommonHead h = new CommonHead();
        assertThat(h.getVersion()).isEqualTo("1.0");
        assertThat(h.getApp()).isEqualTo("HNDEMP");
    }

    @Test
    void srcNodeMustBe14Chars() {
        CommonHead h = new CommonHead();
        assertThatThrownBy(() -> h.setSrcNode("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SrcNode");
    }

    @Test
    void desNodeMustBe14Chars() {
        CommonHead h = new CommonHead();
        assertThatThrownBy(() -> h.setDesNode("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DesNode");
    }

    @Test
    void msgNoMustBeFourDigits() {
        CommonHead h = new CommonHead();
        assertThatThrownBy(() -> h.setMsgNo("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MsgNo");
    }

    @Test
    void workDateMustBeYyyyMmDd() {
        CommonHead h = new CommonHead();
        assertThatThrownBy(() -> h.setWorkDate("2026-04-10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WorkDate");
    }

    @Test
    void nullValuesShouldBeAccepted() {
        CommonHead h = new CommonHead();
        h.setFileName(null);
        h.setFileContentHash(null);
        h.setFileSize(null);
        h.setReserve(null);
        assertThat(h.getFileName()).isNull();
    }
}
