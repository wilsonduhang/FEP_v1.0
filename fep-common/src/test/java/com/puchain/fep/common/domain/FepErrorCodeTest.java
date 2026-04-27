package com.puchain.fep.common.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FepErrorCode} 单元测试。
 *
 * <p>验证错误码唯一性、关键常量存在性与默认消息语义。</p>
 */
class FepErrorCodeTest {

    @Test
    void allErrorCodesShouldBeUnique() {
        Set<String> codes = new HashSet<>();
        for (FepErrorCode ec : FepErrorCode.values()) {
            assertThat(codes.add(ec.getCode()))
                    .as("duplicate code: %s", ec.getCode())
                    .isTrue();
        }
        assertThat(codes).hasSize(FepErrorCode.values().length);
    }

    @Test
    void knownErrorCodesShouldExist() {
        assertThat(FepErrorCode.SUCCESS.getCode()).isEqualTo("200");
        assertThat(FepErrorCode.SYS_0500.getCode()).isEqualTo("SYS_0500");
    }

    @Test
    void reconciliationErrorCodes_shouldBeDefined() {
        assertThat(FepErrorCode.RECON_DAILY_LIMIT_EXCEEDED.getCode()).isEqualTo("RECON_8607");
        assertThat(FepErrorCode.RECON_DAILY_LIMIT_EXCEEDED.getDefaultMessage()).contains("999");
        assertThat(FepErrorCode.RECON_DIR_MAP_MISS.getCode()).isEqualTo("RECON_8608");
        assertThat(FepErrorCode.RECON_DIR_MAP_MISS.getDefaultMessage())
                .contains("MessageDirectionMap");
    }

    @Test
    void converterErrorCodes_shouldBeDefined() {
        assertThat(FepErrorCode.CONV_8001.getCode()).isEqualTo("CONV_8001");
        assertThat(FepErrorCode.CONV_8006.getDefaultMessage()).contains("加密");
        assertThat(FepErrorCode.CONV_8007.getDefaultMessage()).contains("24KB");

        // 覆盖 CONV_8001..CONV_8007 连续存在
        String[] expected = {"CONV_8001", "CONV_8002", "CONV_8003",
                "CONV_8004", "CONV_8005", "CONV_8006", "CONV_8007"};
        Set<String> actual = new HashSet<>();
        for (FepErrorCode ec : FepErrorCode.values()) {
            if (ec.getCode().startsWith("CONV_")) {
                actual.add(ec.getCode());
            }
        }
        assertThat(actual).containsExactlyInAnyOrder(expected);
    }
}
