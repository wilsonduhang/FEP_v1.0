package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link MessageDirectionMap#lookup} 在无 Spring ctx（Bridge.dynamic == null）
 * 场景下走静态 {@link MessageDirectionMap#TABLE} fallback 的行为契约。
 *
 * <p>P3a T4 verifies dynamic-bypass 路径：纯 JUnit 测试，不引入 Spring，Bridge 静态字段
 * 在 {@code @BeforeEach} 显式清空（防 sibling test 副作用污染），
 * {@code MessageDirectionMap.lookup} 直接落到 {@code staticLookup} → TABLE。</p>
 */
class MessageDirectionMapFallbackTest {

    @BeforeEach
    void resetBridge() {
        MessageDirectionMapBridge.clearForTest();
    }

    @AfterEach
    void cleanup() {
        MessageDirectionMapBridge.clearForTest();
    }

    @Test
    void shouldUseStaticFallback_whenNoSpringContext() {
        Optional<DirectionMapping> result = MessageDirectionMap.lookup(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);

        assertThat(result).isPresent();
        assertThat(result.get().direction()).isEqualTo(RoleDirection.INBOUND_PASSIVE);
        assertThat(result.get().mode()).isEqualTo(ProcessingMode.MODE_1);
        assertThat(result.get().requiresFep()).isTrue();
    }

    @Test
    void shouldReturnNotApplicableMapping_for9005AcceptingOrg() {
        // 9005 双角色 NOT_APPLICABLE — 静态 TABLE 命中
        Optional<DirectionMapping> result = MessageDirectionMap.lookup(
                MessageType.MSG_9005, AccessRole.ACCEPTING_ORG);

        assertThat(result).isPresent();
        assertThat(result.get().direction()).isEqualTo(RoleDirection.NOT_APPLICABLE);
    }

    @Test
    void staticLookup_directBypass_returnsSameAsLookup() {
        Optional<DirectionMapping> viaLookup = MessageDirectionMap.lookup(
                MessageType.MSG_3000, AccessRole.INFO_SERVICE_ORG);
        Optional<DirectionMapping> viaStatic = MessageDirectionMap.staticLookup(
                MessageType.MSG_3000, AccessRole.INFO_SERVICE_ORG);

        assertThat(viaLookup).isEqualTo(viaStatic);
        assertThat(viaStatic).isPresent();
        assertThat(viaStatic.get().direction()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE);
        assertThat(viaStatic.get().requiresFep()).isTrue();
    }
}
