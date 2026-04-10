package com.puchain.fep.converter.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestBusinessHeadTest {

    @Test
    void validFields_shouldBeAccepted() {
        RequestBusinessHead h = new RequestBusinessHead();
        h.setSendOrgCode("12345678901234");
        h.setEntrustDate("20260410");
        h.setTransitionNo("00000001");
        assertThat(h.getSendOrgCode()).isEqualTo("12345678901234");
        assertThat(h.getEntrustDate()).isEqualTo("20260410");
        assertThat(h.getTransitionNo()).isEqualTo("00000001");
    }

    @Test
    void sendOrgCodeMustBe14Chars() {
        RequestBusinessHead h = new RequestBusinessHead();
        assertThatThrownBy(() -> h.setSendOrgCode("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SendOrgCode");
    }

    @Test
    void entrustDateMustBeYyyyMmDd() {
        RequestBusinessHead h = new RequestBusinessHead();
        assertThatThrownBy(() -> h.setEntrustDate("2026-04-10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EntrustDate");
    }

    @Test
    void transitionNoMustBe8Digits() {
        RequestBusinessHead h = new RequestBusinessHead();
        assertThatThrownBy(() -> h.setTransitionNo("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TransitionNo");
    }

    @Test
    void nullValuesShouldBeAccepted() {
        RequestBusinessHead h = new RequestBusinessHead();
        h.setSendOrgCode(null);
        h.setEntrustDate(null);
        h.setTransitionNo(null);
        assertThat(h.getSendOrgCode()).isNull();
        assertThat(h.getEntrustDate()).isNull();
        assertThat(h.getTransitionNo()).isNull();
    }
}
