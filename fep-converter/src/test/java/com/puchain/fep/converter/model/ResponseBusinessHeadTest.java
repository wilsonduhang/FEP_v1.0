package com.puchain.fep.converter.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseBusinessHeadTest {

    @Test
    void inheritsRequestFields() {
        ResponseBusinessHead h = new ResponseBusinessHead();
        h.setSendOrgCode("12345678901234");
        h.setEntrustDate("20260410");
        h.setTransitionNo("00000001");
        h.setResult("90000");
        h.setAddWord("业务受理成功");
        assertThat(h.getSendOrgCode()).isEqualTo("12345678901234");
        assertThat(h.getResult()).isEqualTo("90000");
        assertThat(h.getAddWord()).isEqualTo("业务受理成功");
    }

    @Test
    void resultMustBe5Digits() {
        ResponseBusinessHead h = new ResponseBusinessHead();
        assertThatThrownBy(() -> h.setResult("12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Result");
    }

    @Test
    void addWordShouldRejectOver200Chars() {
        ResponseBusinessHead h = new ResponseBusinessHead();
        String over = "x".repeat(201);
        assertThatThrownBy(() -> h.setAddWord(over))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AddWord");
    }

    @Test
    void addWord200CharsShouldBeAccepted() {
        ResponseBusinessHead h = new ResponseBusinessHead();
        String exactly200 = "x".repeat(200);
        h.setAddWord(exactly200);
        assertThat(h.getAddWord()).hasSize(200);
    }

    @Test
    void addWordNullShouldBeAccepted() {
        ResponseBusinessHead h = new ResponseBusinessHead();
        h.setAddWord(null);
        assertThat(h.getAddWord()).isNull();
    }
}
