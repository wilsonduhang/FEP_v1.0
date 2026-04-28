package com.puchain.fep.web.tlq.node.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.tlq.node.domain.TlqNodeRole;
import com.puchain.fep.web.tlq.node.domain.TlqNodeStatus;
import com.puchain.fep.web.tlq.node.dto.TlqNodeCreateRequest;
import com.puchain.fep.web.tlq.node.dto.TlqNodeResponse;
import com.puchain.fep.web.tlq.node.dto.TlqNodeUpdateRequest;
import com.puchain.fep.web.tlq.node.service.TlqNodeLoginService;
import com.puchain.fep.web.tlq.node.service.TlqNodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TlqNodeController 集成测试（MockBean 服务层，跳过 Security 过滤器）。
 *
 * <p>覆盖 6 个 REST 端点的路由映射和响应结构校验。
 * 参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TlqNodeControllerTest {

    private static final String BASE_URL = "/api/v1/tlq/nodes";
    private static final String NODE_ID = "node-test-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TlqNodeService tlqNodeService;

    @MockBean
    private TlqNodeLoginService tlqNodeLoginService;

    /**
     * POST /api/v1/tlq/nodes — 创建节点返回 201，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void createNode_returns201() throws Exception {
        TlqNodeResponse response = new TlqNodeResponse();
        when(tlqNodeService.createNode(any(TlqNodeCreateRequest.class))).thenReturn(response);

        TlqNodeCreateRequest request = new TlqNodeCreateRequest();
        request.setNodeName("primary-node");
        request.setNodeRole(TlqNodeRole.MASTER_PRODUCER);
        request.setHostIp("192.168.1.10");
        request.setPort(20001);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * GET /api/v1/tlq/nodes/{id} — 查询节点详情返回 200，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getNode_returns200() throws Exception {
        TlqNodeResponse response = new TlqNodeResponse();
        when(tlqNodeService.getNode(NODE_ID)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/" + NODE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * GET /api/v1/tlq/nodes?page=0&size=20 — 分页查询返回 200，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void listNodes_returns200() throws Exception {
        PageResult<TlqNodeResponse> pageResult =
                new PageResult<>(List.of(), 0L, 1, 20);
        when(tlqNodeService.listNodes(any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResult);

        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * PUT /api/v1/tlq/nodes/{id} — 更新节点返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void updateNode_returns200() throws Exception {
        TlqNodeResponse response = new TlqNodeResponse();
        when(tlqNodeService.updateNode(eq(NODE_ID), any(TlqNodeUpdateRequest.class)))
                .thenReturn(response);

        TlqNodeUpdateRequest request = new TlqNodeUpdateRequest();
        request.setDescription("updated description");

        mockMvc.perform(put(BASE_URL + "/" + NODE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * DELETE /api/v1/tlq/nodes/{id} — 删除节点返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void deleteNode_returns200() throws Exception {
        doNothing().when(tlqNodeService).deleteNode(NODE_ID);

        mockMvc.perform(delete(BASE_URL + "/" + NODE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * PATCH /api/v1/tlq/nodes/{id}/status?target=ONLINE — 切换状态返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void changeStatus_returns200() throws Exception {
        TlqNodeResponse response = new TlqNodeResponse();
        when(tlqNodeService.changeStatus(eq(NODE_ID), eq(TlqNodeStatus.ONLINE)))
                .thenReturn(response);

        mockMvc.perform(patch(BASE_URL + "/" + NODE_ID + "/status")
                        .param("target", "ONLINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * POST /api/v1/tlq/nodes/{id}/login — 9006 编排成功返回 200，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void login_success_returns200() throws Exception {
        when(tlqNodeLoginService.login(NODE_ID)).thenReturn(true);
        TlqNodeResponse response = new TlqNodeResponse();
        when(tlqNodeService.getNode(NODE_ID)).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/" + NODE_ID + "/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * POST /api/v1/tlq/nodes/{id}/login — 9006 编排失败返回 200 但 $.code=TRANS_7003。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void login_failure_returnsErrorCode() throws Exception {
        when(tlqNodeLoginService.login(NODE_ID)).thenReturn(false);

        mockMvc.perform(post(BASE_URL + "/" + NODE_ID + "/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TRANS_7003"));
    }

    /**
     * POST /api/v1/tlq/nodes/{id}/logout — 9008 编排成功返回 200，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void logout_success_returns200() throws Exception {
        when(tlqNodeLoginService.logout(NODE_ID)).thenReturn(true);
        TlqNodeResponse response = new TlqNodeResponse();
        when(tlqNodeService.getNode(NODE_ID)).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/" + NODE_ID + "/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
