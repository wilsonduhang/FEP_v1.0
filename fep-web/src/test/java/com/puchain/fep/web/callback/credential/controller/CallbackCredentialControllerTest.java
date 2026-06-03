package com.puchain.fep.web.callback.credential.controller;

import com.puchain.fep.common.exception.GlobalExceptionHandler;
import com.puchain.fep.web.callback.credential.dto.CredentialResponse;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialAdminService;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 单元测试 for {@link CallbackCredentialController}.
 *
 * <p>采用 {@link MockMvcBuilders#standaloneSetup} 而非 {@code @WebMvcTest}：后者会加载
 * {@code JpaConfiguration} 触发 JPA + Security bean 装配（fep-web slice 不可 mock），
 * 与既有 {@code DirMapConfigControllerTest} 同型规避。standaloneSetup 仍真实驱动
 * {@code @RequestMapping} dispatch + Jackson 序列化，足以验证安全核心断言：
 * 响应 JSON 绝不含任何密文字段。{@code @PreAuthorize} 鉴权在 standalone 模式不生效，
 * 故 403 鉴权路径由完整 Spring Security IT 覆盖（本类聚焦序列化形状）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackCredentialControllerTest {

    private CallbackCredentialAdminService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CallbackCredentialAdminService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CallbackCredentialController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postCreateReturnsResponseWithoutCiphertext() throws Exception {
        final CredentialResponse resp = new CredentialResponse();
        resp.setCredentialId("CRED-001");
        resp.setInterfaceId("IF-001");
        resp.setAuthType(InterfaceAuthType.TOKEN);
        resp.setTokenConfigured(true);
        when(service.create(any())).thenReturn(resp);

        mvc.perform(post("/api/v1/callback/credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interfaceId\":\"IF-001\",\"authType\":\"TOKEN\",\"token\":\"plain\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credentialId").value("CRED-001"))
                .andExpect(jsonPath("$.data.tokenConfigured").value(true))
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.tokenCiphertext").doesNotExist())
                .andExpect(jsonPath("$.data.oauthClientIdCiphertext").doesNotExist())
                .andExpect(jsonPath("$.data.oauthClientSecretCiphertext").doesNotExist());
    }

    @Test
    void getReturnsResponseWithoutCiphertext() throws Exception {
        final CredentialResponse resp = new CredentialResponse();
        resp.setCredentialId("CRED-002");
        resp.setInterfaceId("IF-001");
        resp.setAuthType(InterfaceAuthType.OAUTH2);
        resp.setOauthClientIdConfigured(true);
        resp.setOauthClientSecretConfigured(true);
        when(service.get("IF-001")).thenReturn(resp);

        mvc.perform(get("/api/v1/callback/credentials/IF-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.oauthClientIdConfigured").value(true))
                .andExpect(jsonPath("$.data.oauthClientId").doesNotExist())
                .andExpect(jsonPath("$.data.oauthClientSecret").doesNotExist())
                .andExpect(jsonPath("$.data.oauthClientIdCiphertext").doesNotExist())
                .andExpect(jsonPath("$.data.oauthClientSecretCiphertext").doesNotExist());
    }

    @Test
    void deleteInvokesServiceAndReturnsOk() throws Exception {
        mvc.perform(delete("/api/v1/callback/credentials/IF-001"))
                .andExpect(status().isOk());

        verify(service).delete(eq("IF-001"));
    }
}
