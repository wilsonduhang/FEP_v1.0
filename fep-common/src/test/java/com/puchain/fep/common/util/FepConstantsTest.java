package com.puchain.fep.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FepConstants}.
 *
 * <p>R-2 (2026-05-07): introduce {@code HNDEMP_NODE_CODE} constant to replace
 * the 109-occurrence hardcoded literal {@code "A1000143000104"} across the
 * codebase. Decision: {@code docs/plans/2026-05-06-p5-v2-cleanup-r1-r2-b1-umbrella.md} §3 T2.</p>
 */
class FepConstantsTest {

    @Test
    @DisplayName("HNDEMP_NODE_CODE 必须为固定 14 字符 PRD 中心节点代码 A1000143000104")
    void hndempNodeCode_shouldBeFixedValue() {
        assertThat(FepConstants.HNDEMP_NODE_CODE)
            .isEqualTo("A1000143000104")
            .hasSize(14);
    }

    @Test
    @DisplayName("FepConstants 必须为 final 工具类，私有构造防止反射实例化")
    void privateConstructor_shouldPreventInstantiation() throws NoSuchMethodException {
        assertThat(Modifier.isFinal(FepConstants.class.getModifiers())).isTrue();

        Constructor<FepConstants> ctor = FepConstants.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();

        ctor.setAccessible(true);
        assertThatThrownBy(ctor::newInstance)
            .isInstanceOf(InvocationTargetException.class)
            .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
