package com.puchain.fep.converter.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SerialNoBearing} 接口契约测试。
 *
 * <p>本测试不验证 dispatcher 行为（dispatcher null/empty 处理在
 * {@code InboundMessageDispatcherSerialNoTest}）。仅验证接口本身：</p>
 * <ul>
 *   <li>单方法签名 {@code String getSerialNo()}</li>
 *   <li>实现方返回任意 String（含 null / empty）均合法 — 接口不约束</li>
 * </ul>
 */
class SerialNoBearingTest {

    @Test
    void lambdaImplementation_returnsStringValue() {
        SerialNoBearing bearer = () -> "ABC12345";
        assertThat(bearer.getSerialNo()).isEqualTo("ABC12345");
    }

    @Test
    void lambdaImplementation_returnsNull_isAllowed() {
        SerialNoBearing bearer = () -> null;
        assertThat(bearer.getSerialNo()).isNull();
    }

    @Test
    void lambdaImplementation_returnsEmpty_isAllowed() {
        SerialNoBearing bearer = () -> "";
        assertThat(bearer.getSerialNo()).isEmpty();
    }
}
