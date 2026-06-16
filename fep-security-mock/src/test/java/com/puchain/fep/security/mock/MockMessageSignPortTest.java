package com.puchain.fep.security.mock;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MockMessageSignPort 宽松语义测试：固定签名常量、验签恒 true、null 参数防御。
 */
class MockMessageSignPortTest {

    private final MockMessageSignPort port = new MockMessageSignPort();
    private static final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);

    @Test
    void sign_returnsMockConstant() {
        assertThat(port.sign(DATA)).isEqualTo(MockMessageSignPort.MOCK_MSG_SIGN);
    }

    @Test
    void verify_alwaysReturnsTrue() {
        assertThat(port.verify(DATA, "any-signature", "ANY_NODE")).isTrue();
    }

    @Test
    void sign_nullData_throwsIllegalArgument() {
        assertThatThrownBy(() -> port.sign(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verify_nullArgs_throwIllegalArgument() {
        assertThatThrownBy(() -> port.verify(null, "s", "n"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> port.verify(DATA, null, "n"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> port.verify(DATA, "s", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
