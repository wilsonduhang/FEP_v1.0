package com.puchain.fep.web.tlq.queue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.tlq.queue.domain.TlqChannelType;
import com.puchain.fep.web.tlq.queue.domain.TlqQueueType;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueBatchGenerateRequest;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueConfigCreateRequest;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueConfigResponse;
import com.puchain.fep.web.tlq.queue.service.TlqQueueConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TlqQueueConfigController 集成测试（MockBean 服务层，跳过 Security 过滤器）。
 *
 * <p>覆盖 4 个 REST 端点的路由映射和响应结构校验。
 * 参见 PRD v1.3 §3.1.2 TLQ 队列管理（FR-WEB-TLQ-QUEUE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TlqQueueConfigControllerTest {

    private static final String BASE_URL = "/api/v1/tlq/queues";
    private static final String QUEUE_ID = "queue-test-001";
    private static final String NODE_ID = "node-test-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TlqQueueConfigService queueConfigService;

    /**
     * POST /api/v1/tlq/queues — 创建队列配置返回 201，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void createQueue_returns201() throws Exception {
        TlqQueueConfigResponse response = new TlqQueueConfigResponse();
        when(queueConfigService.createQueue(any(TlqQueueConfigCreateRequest.class)))
                .thenReturn(response);

        TlqQueueConfigCreateRequest request = new TlqQueueConfigCreateRequest();
        request.setNodeId(NODE_ID);
        request.setQueueName("FEP.SEND.QUEUE");
        request.setChannelType(TlqChannelType.REALTIME);
        request.setQueueType(TlqQueueType.LOCAL);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * POST /api/v1/tlq/queues/batch-generate — 批量生成标准队列返回 201，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void batchGenerate_returns201() throws Exception {
        when(queueConfigService.batchGenerateQueues(any(TlqQueueBatchGenerateRequest.class)))
                .thenReturn(List.of());

        TlqQueueBatchGenerateRequest request = new TlqQueueBatchGenerateRequest();
        request.setNodeId(NODE_ID);
        request.setOrganizationCode("91430100XXXXXXXXXX");

        mockMvc.perform(post(BASE_URL + "/batch-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * GET /api/v1/tlq/queues?nodeId=xxx — 按节点查询队列列表返回 200，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void listByNode_returns200() throws Exception {
        when(queueConfigService.listByNode(NODE_ID)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL)
                        .param("nodeId", NODE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * DELETE /api/v1/tlq/queues/{id} — 删除队列配置返回 200，$.code=200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void deleteQueue_returns200() throws Exception {
        doNothing().when(queueConfigService).deleteQueue(QUEUE_ID);

        mockMvc.perform(delete(BASE_URL + "/" + QUEUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
