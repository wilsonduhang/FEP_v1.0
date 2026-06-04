package com.puchain.fep.web.callback.notification.controller;

import com.puchain.fep.common.exception.GlobalExceptionHandler;
import com.puchain.fep.web.callback.notification.dto.CallbackNotificationResponse;
import com.puchain.fep.web.callback.notification.service.CallbackNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 单元测试 for {@link CallbackNotificationController}。
 *
 * <p>采用 {@link MockMvcBuilders#standaloneSetup}（同 {@code CallbackDlqControllerTest} /
 * {@code CallbackCredentialControllerTest} 规避 {@code @WebMvcTest} JpaConfiguration 装配），
 * 真实驱动 dispatch + Jackson 序列化 + Principal 参数解析。验证端点取认证 username 作用户边界
 * 并正确委派服务。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackNotificationControllerTest {

    private CallbackNotificationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CallbackNotificationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CallbackNotificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getUnreadReturnsCurrentUserNotifications() throws Exception {
        when(service.listUnread("alice")).thenReturn(List.of(
                new CallbackNotificationResponse("N1", "CALLBACK_DLQ", "ERROR", "回调死信 - IF-001",
                        "queueId=Q1", "Q1", "CALLBACK_DLQ_ENTRY", false, LocalDateTime.now(), null)));

        mvc.perform(get("/api/v1/notifications/unread")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].notificationId").value("N1"))
                .andExpect(jsonPath("$.data[0].category").value("CALLBACK_DLQ"))
                .andExpect(jsonPath("$.data[0].read").value(false));

        verify(service).listUnread("alice");
    }

    @Test
    void getUnreadCountReturnsCount() throws Exception {
        when(service.unreadCount("alice")).thenReturn(7L);

        mvc.perform(get("/api/v1/notifications/unread/count")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    void putMarkReadPassesUsernameToService() throws Exception {
        mvc.perform(put("/api/v1/notifications/N1/read")
                        .principal(new UsernamePasswordAuthenticationToken("alice", "n")))
                .andExpect(status().isOk());

        verify(service).markRead(eq("N1"), eq("alice"));
    }
}
