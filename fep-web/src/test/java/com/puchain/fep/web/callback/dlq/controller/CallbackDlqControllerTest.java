package com.puchain.fep.web.callback.dlq.controller;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.exception.GlobalExceptionHandler;
import com.puchain.fep.web.callback.dlq.dto.DlqEntryResponse;
import com.puchain.fep.web.callback.dlq.dto.DlqReplayResponse;
import com.puchain.fep.web.callback.dlq.service.CallbackReplayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 单元测试 for {@link CallbackDlqController}。
 *
 * <p>采用 {@link MockMvcBuilders#standaloneSetup} 而非 {@code @WebMvcTest}：后者会加载
 * {@code JpaConfiguration} 触发 JPA + Security bean 装配（fep-web slice 不可 mock），与既有
 * {@code CallbackCredentialControllerTest} / {@code DirMapConfigControllerTest} 同型规避。
 * standaloneSetup 仍真实驱动 {@code @RequestMapping} dispatch + Jackson 序列化 + Principal
 * 参数解析 + {@code GlobalExceptionHandler}。{@code @PreAuthorize} 鉴权在 standalone 模式
 * 不生效，故 403 非管理员路径由完整 Spring Security IT 覆盖（本类聚焦委派 + 序列化 + 错误映射）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackDlqControllerTest {

    private CallbackReplayService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CallbackReplayService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CallbackDlqController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void postReplayPassesUsernameToService() throws Exception {
        when(service.replay(eq("D1"), eq("admin-x"))).thenReturn(
                new DlqReplayResponse("NEW-001", "D1", LocalDateTime.now()));

        mvc.perform(post("/api/v1/callback/dlq/D1/replay")
                        .principal(new UsernamePasswordAuthenticationToken("admin-x", "n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newQueueId").value("NEW-001"))
                .andExpect(jsonPath("$.data.originalDlqId").value("D1"));

        verify(service).replay("D1", "admin-x");
    }

    @Test
    void postReplayNotFoundMapsTo404() throws Exception {
        when(service.replay(eq("D-NOPE"), any())).thenThrow(
                new FepBusinessException(FepErrorCode.BIZ_5001, "DLQ entry not found, id=D-NOPE"));

        mvc.perform(post("/api/v1/callback/dlq/D-NOPE/replay")
                        .principal(new UsernamePasswordAuthenticationToken("admin-x", "n")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BIZ_5001"));
    }

    @Test
    void getListReturnsEntries() throws Exception {
        when(service.list(any())).thenReturn(List.of(
                new DlqEntryResponse("D1", "IF-001", "2101", "DEAD_LETTER", 5, "fatal",
                        LocalDateTime.now(), null, null, null)));

        mvc.perform(get("/api/v1/callback/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].queueId").value("D1"))
                .andExpect(jsonPath("$.data[0].status").value("DEAD_LETTER"));
    }

    @Test
    void getChainReturnsLinkedEntries() throws Exception {
        when(service.findReplayChain("D1")).thenReturn(List.of(
                new DlqEntryResponse("D2", "IF-001", "2101", "PENDING", 0, null,
                        LocalDateTime.now(), "D1", "admin-x", LocalDateTime.now())));

        mvc.perform(get("/api/v1/callback/dlq/D1/chain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].queueId").value("D2"))
                .andExpect(jsonPath("$.data[0].originalDlqId").value("D1"));
    }
}
