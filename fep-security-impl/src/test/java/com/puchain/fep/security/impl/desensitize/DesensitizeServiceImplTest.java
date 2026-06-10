package com.puchain.fep.security.impl.desensitize;

import com.puchain.fep.security.api.DesensitizeService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DesensitizeServiceImpl 脱敏规则（架构 §680-682）+ 边界验证。
 */
class DesensitizeServiceImplTest {

    private final DesensitizeService svc = new DesensitizeServiceImpl();

    @Test
    void maskIdCard_keepsFirst3Last4() {
        assertThat(svc.maskIdCard("110101199003078888")).isEqualTo("110***********8888");
    }

    @Test
    void maskBankCard_19digits_keepsFirst6Last4() {
        assertThat(svc.maskBankCard("6222021234567890123")).isEqualTo("622202*********0123");
    }

    @Test
    void maskBankCard_16digits_keepsFirst6Last4() {
        assertThat(svc.maskBankCard("6222021234567890")).isEqualTo("622202******7890");
    }

    @Test
    void maskPhone_keepsFirst3Last4() {
        assertThat(svc.maskPhone("13800138000")).isEqualTo("138****8000");
    }

    @Test
    void maskUsci_keepsLast4() {
        assertThat(svc.maskUsci("91110108MA01XK7A88")).isEqualTo("**************7A88");
    }

    @Test
    void mask_null_returnsNA() {
        assertThat(svc.maskIdCard(null)).isEqualTo("N/A");
        assertThat(svc.maskPhone("  ")).isEqualTo("N/A");
    }

    @Test
    void mask_tooShort_masksFully() {
        // 长度 ≤ 保留位数和 → 全掩不泄漏
        assertThat(svc.maskIdCard("123")).isEqualTo("***");
        assertThat(svc.maskPhone("12345")).isEqualTo("*****");
    }
}
