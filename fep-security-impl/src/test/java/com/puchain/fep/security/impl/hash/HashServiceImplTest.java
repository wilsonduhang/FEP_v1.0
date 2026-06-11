package com.puchain.fep.security.impl.hash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * SM3 摘要 GB/T 32905-2016 附录 A 标准向量逐字节验证。
 */
class HashServiceImplTest {

    private final HashServiceImpl hashService = new HashServiceImpl();

    @Test
    void sm3Hex_abcVector_matchesGbt32905AppendixA1() {
        assertThat(hashService.sm3Hex("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");
    }

    @Test
    void sm3Hex_abcd16Vector_matchesGbt32905AppendixA2() {
        assertThat(hashService.sm3Hex("abcd".repeat(16).getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732");
    }

    @Test
    void sm3Hex_nullData_throwsIllegalArgument() {
        assertThatThrownBy(() -> hashService.sm3Hex(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sm3Hex_emptyData_returns64LowercaseHexChars() {
        assertThat(hashService.sm3Hex(new byte[0])).matches("[0-9a-f]{64}");
    }
}
