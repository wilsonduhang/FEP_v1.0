package com.puchain.fep.web.tlq.connectivity.controller;

import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.tlq.connectivity.domain.ConnectivityTestResult;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivityRecordResponse;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivitySummaryResponse;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivityTestResponse;
import com.puchain.fep.web.tlq.connectivity.service.TlqConnectivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TlqConnectivityController 集成测试（MockBean 服务层，跳过 Security 过滤器）。
 *
 * <p>覆盖 3 个 REST 端点的路由映射和响应结构校验。
 * 参见 PRD v1.3 §5.7.5 TLQ 连通性监控（FR-WEB-TLQ-CONN）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TlqConnectivityControllerTest {

    private static final String BASE_URL = "/api/v1/tlq/connectivity";
    private static final String NODE_ID = "node-test-conn-001";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TlqConnectivityService connectivityService;

    /**
     * POST /api/v1/tlq/connectivity/{nodeId}/test — 触发测试返回 201，$.data.result=SUCCESS。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void triggerTest_returns201() throws Exception {
        ConnectivityTestResponse response = new ConnectivityTestResponse();
        response.setNodeId(NODE_ID);
        response.setResult(ConnectivityTestResult.SUCCESS);
        response.setRttMs(0);
        when(connectivityService.triggerTest(NODE_ID)).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/" + NODE_ID + "/test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.result").value("SUCCESS"));
    }

    /**
     * GET /api/v1/tlq/connectivity/{nodeId}/records — 分页查询历史记录返回 200，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void listRecords_returns200() throws Exception {
        Page<ConnectivityRecordResponse> page =
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(connectivityService.listRecords(eq(NODE_ID), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get(BASE_URL + "/" + NODE_ID + "/records")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * GET /api/v1/tlq/connectivity/{nodeId}/summary — 获取统计汇总返回 200，$.data.successRate=80.0。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getSummary_returns200() throws Exception {
        ConnectivitySummaryResponse summary = new ConnectivitySummaryResponse();
        summary.setNodeId(NODE_ID);
        summary.setTotalTests(10L);
        summary.setSuccessCount(8L);
        summary.setSuccessRate(80.0);
        when(connectivityService.getSummary(NODE_ID)).thenReturn(summary);

        mockMvc.perform(get(BASE_URL + "/" + NODE_ID + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.successRate").value(80.0));
    }
}
