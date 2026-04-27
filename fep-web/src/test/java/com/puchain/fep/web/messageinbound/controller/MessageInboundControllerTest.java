package com.puchain.fep.web.messageinbound.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.messageinbound.dto.InboundMessageRequest;
import com.puchain.fep.web.messageinbound.dto.InboundMessageResponse;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc test for {@link MessageInboundController}.
 *
 * <p>Covers 5 cases (P3 Task 2 v1a verification §6):</p>
 * <ol>
 *   <li>3116 valid → 200 + recordId echoed</li>
 *   <li>3115 valid → 200 + recordId echoed</li>
 *   <li>unknown messageType=9999 → HTTP 400 + ApiResult.code=MSG_8701
 *       via existing {@code GlobalExceptionHandler.handleBusiness}
 *       mapping {@link FepBusinessException} → BAD_REQUEST</li>
 *   <li>xmlBase64 decode failure → HTTP 400 + ApiResult.code=MSG_8702</li>
 *   <li>pipeline FAILED → HTTP 200 + eventPublished=false</li>
 * </ol>
 *
 * <p>Uses {@code @SpringBootTest + @AutoConfigureMockMvc(addFilters = false)}
 * to stay aligned with the existing TLQ controller test precedents
 * ({@code TlqNodeControllerTest}, {@code TlqQueueConfigControllerTest}).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MessageInboundControllerTest {

    private static final String BASE_URL = "/api/v1/messages/inbound";
    private static final String VALID_BASE64_XML =
            Base64.getEncoder().encodeToString("<CFX/>".getBytes());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InboundMessageDispatcher dispatcher;

    @Test
    @DisplayName("POST /messages/inbound 3116 valid → 200 + recordId echoed")
    void handleInbound_3116Valid_returns200WithRecordId() throws Exception {
        final InboundMessageResponse response = new InboundMessageResponse(
                "rec-3116-abcdef0123456789abcdef0123", "COMPLETED", true);
        when(dispatcher.dispatch(eq("3116"), eq("20260428"), any(byte[].class)))
                .thenReturn(response);

        final InboundMessageRequest req = new InboundMessageRequest();
        req.setMessageType("3116");
        req.setTransitionNo("20260428");
        req.setXmlBase64(VALID_BASE64_XML);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.recordId").value("rec-3116-abcdef0123456789abcdef0123"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.eventPublished").value(true));
    }

    @Test
    @DisplayName("POST /messages/inbound 3115 valid → 200 + recordId echoed")
    void handleInbound_3115Valid_returns200WithRecordId() throws Exception {
        final InboundMessageResponse response = new InboundMessageResponse(
                "rec-3115-abcdef0123456789abcdef0123", "COMPLETED", true);
        when(dispatcher.dispatch(eq("3115"), eq("20260428"), any(byte[].class)))
                .thenReturn(response);

        final InboundMessageRequest req = new InboundMessageRequest();
        req.setMessageType("3115");
        req.setTransitionNo("20260428");
        req.setXmlBase64(VALID_BASE64_XML);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value("rec-3115-abcdef0123456789abcdef0123"));
    }

    @Test
    @DisplayName("POST /messages/inbound unknown messageType=9999 → 400 + code=MSG_8701")
    void handleInbound_unknownMessageType_returns400WithMsg8701() throws Exception {
        when(dispatcher.dispatch(eq("9999"), eq("20260428"), any(byte[].class)))
                .thenThrow(new FepBusinessException(
                        FepErrorCode.MSG_INBOUND_INVALID_TYPE,
                        "messageType=9999"));

        final InboundMessageRequest req = new InboundMessageRequest();
        req.setMessageType("9999");
        req.setTransitionNo("20260428");
        req.setXmlBase64(VALID_BASE64_XML);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MSG_8701"));
    }

    @Test
    @DisplayName("POST /messages/inbound malformed xmlBase64 → 400 + code=MSG_8702")
    void handleInbound_malformedBase64_returns400WithMsg8702() throws Exception {
        final InboundMessageRequest req = new InboundMessageRequest();
        req.setMessageType("3116");
        req.setTransitionNo("20260428");
        // Force IllegalArgumentException inside the Base64 decoder.
        req.setXmlBase64("!!!not-base64!!!");

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MSG_8702"));
    }

    @Test
    @DisplayName("POST /messages/inbound pipeline FAILED → 200 + eventPublished=false")
    void handleInbound_pipelineFailed_returns200WithEventPublishedFalse() throws Exception {
        final InboundMessageResponse response = new InboundMessageResponse(
                "rec-3116-failedabcdef0123456789ab", "FAILED", false);
        when(dispatcher.dispatch(eq("3116"), eq("20260428"), any(byte[].class)))
                .thenReturn(response);

        final InboundMessageRequest req = new InboundMessageRequest();
        req.setMessageType("3116");
        req.setTransitionNo("20260428");
        req.setXmlBase64(VALID_BASE64_XML);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.eventPublished").value(false));
    }
}
