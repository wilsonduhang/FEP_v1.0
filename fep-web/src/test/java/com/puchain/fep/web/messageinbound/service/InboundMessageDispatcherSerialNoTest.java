package com.puchain.fep.web.messageinbound.service;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E-3 T3 — {@code InboundMessageDispatcher.extractSerialNo} 5 分支单测。
 *
 * <p>v0.3 修订（santa Round 2 Reviewer C' S1）— extractSerialNo 改 package-private
 * 后本测试与 dispatcher 同包，直接 static call，无 Method.invoke 反射。这避免了
 * v0.1/v0.2 'test-only 反射' 与 'refactor 替换反射' 自相矛盾的设计 smell。</p>
 */
class InboundMessageDispatcherSerialNoTest {

    private static final String TRANSITION_NO = "TX000001";

    @Test
    void nullBody_returnsTransitionNo() {
        assertThat(InboundMessageDispatcher.extractSerialNo(null, TRANSITION_NO))
                .isEqualTo(TRANSITION_NO);
    }

    @Test
    void nonSerialNoBearingCfxBody_returnsTransitionNo() {
        // 嵌套结构 CfxBody 不实现 SerialNoBearing — 应走 fallback
        CfxBody nested = new CfxBody() { };
        assertThat(InboundMessageDispatcher.extractSerialNo(nested, TRANSITION_NO))
                .isEqualTo(TRANSITION_NO);
    }

    @Test
    void serialNoBearing_returnsNull_returnsTransitionNo() {
        SerialNoBearing bearer = () -> null;
        assertThat(InboundMessageDispatcher.extractSerialNo(bearer, TRANSITION_NO))
                .isEqualTo(TRANSITION_NO);
    }

    @Test
    void serialNoBearing_returnsEmpty_returnsTransitionNo() {
        SerialNoBearing bearer = () -> "";
        assertThat(InboundMessageDispatcher.extractSerialNo(bearer, TRANSITION_NO))
                .isEqualTo(TRANSITION_NO);
    }

    @Test
    void serialNoBearing_returnsValue_returnsValue() {
        SerialNoBearing bearer = () -> "SN999999";
        assertThat(InboundMessageDispatcher.extractSerialNo(bearer, TRANSITION_NO))
                .isEqualTo("SN999999");
    }
}
