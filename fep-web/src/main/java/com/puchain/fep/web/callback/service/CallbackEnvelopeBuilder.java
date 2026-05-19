package com.puchain.fep.web.callback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 {@link InboundMessageProcessedEvent} 业务体封装为 PRD §7.1 line 2143
 * 统一响应封套 JSON：{@code {code,message,data,traceId,timestamp}}。
 *
 * <p>timestamp 通过注入的 {@link Clock} 生成，确保单元测试可控时间。
 * traceId 取自 {@link InboundMessageProcessedEvent#serialNo()}，作为业务流水号。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackEnvelopeBuilder {

    private static final String SUCCESS_CODE = "200";
    private static final String SUCCESS_MSG = "成功";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * @param objectMapper Jackson 序列化器，非空
     * @param clock        注入以可测 timestamp，非空
     */
    public CallbackEnvelopeBuilder(final ObjectMapper objectMapper, final Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 构建 PRD §7.1 line 2143 统一封套 JSON。
     *
     * <p>body 为 {@code null} 时 data 字段输出 {@code null}（pipeline 已落库，
     * 回调仍通知"收妥无体"）。</p>
     *
     * @param event inbound 已处理事件，非空
     * @return §7.1 统一封套 JSON 串
     * @throws FepBusinessException 序列化失败（{@link FepErrorCode#CONV_8001}）
     */
    public String build(final InboundMessageProcessedEvent event) {
        final Map<String, Object> env = new LinkedHashMap<>();
        env.put("code", SUCCESS_CODE);
        env.put("message", SUCCESS_MSG);
        env.put("data", event.body());
        env.put("traceId", event.serialNo());
        env.put("timestamp", OffsetDateTime.now(clock)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        try {
            return objectMapper.writeValueAsString(env);
        } catch (final JsonProcessingException ex) {
            throw new FepBusinessException(FepErrorCode.CONV_8001,
                    "callback envelope serialize failed for serialNo=" + event.serialNo(), ex);
        }
    }
}
