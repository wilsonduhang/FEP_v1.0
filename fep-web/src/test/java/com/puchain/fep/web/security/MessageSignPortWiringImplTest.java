package com.puchain.fep.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.security.api.MessageSignPort;
import com.puchain.fep.security.api.SignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * GM S2b provider=impl 装配断言：{@link MessageSignPort} 解析为 BcMessageSignPort 单 bean
 * （impl 侧 GmSecurityConfiguration @Bean，无 stereotype，门控 provider=impl）。
 *
 * <p>断言经 api 接口 + 简单类名（不引 impl 类，守 fep-web→impl runtime scope 与 ArchUnit R5）。
 * @MockBean SignService 镜像既有 impl-provider IT 范式（全 context 启动护栏）。命名 *Test 入 Surefire。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "fep.security.provider=impl",
        "fep.security.sm4.active-key-id=sm4-cred-v1",
        "fep.security.sm4.sm4-keys.sm4-cred-v1=0123456789abcdeffedcba9876543210"
})
class MessageSignPortWiringImplTest {

    @Autowired
    private MessageSignPort messageSignPort;

    @MockBean
    private SignService signService;

    @Test
    void implProvider_wiresBcMessageSignPort() {
        assertThat(messageSignPort.getClass().getSimpleName()).isEqualTo("BcMessageSignPort");
    }
}
