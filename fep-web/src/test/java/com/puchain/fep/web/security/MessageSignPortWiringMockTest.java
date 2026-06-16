package com.puchain.fep.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.security.api.MessageSignPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * GM S2b 默认（mock）provider 装配断言：{@link MessageSignPort} 解析为 MockMessageSignPort
 * 单 bean（@ConditionalOnProperty matchIfMissing=true，与 impl 互斥）。
 *
 * <p>默认 dev profile + 未设 provider → mock 兜底。断言经 api 接口 + 简单类名。命名 *Test 入 Surefire。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
class MessageSignPortWiringMockTest {

    @Autowired
    private MessageSignPort messageSignPort;

    @Test
    void defaultProvider_wiresMockMessageSignPort() {
        assertThat(messageSignPort.getClass().getSimpleName()).isEqualTo("MockMessageSignPort");
    }
}
