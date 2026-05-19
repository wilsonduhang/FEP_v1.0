package com.puchain.fep.web.callback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackEnvelopeBuilderTest {

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-05-19T06:30:00Z"), ZoneId.of("Asia/Shanghai"));
    private final CallbackEnvelopeBuilder builder =
            new CallbackEnvelopeBuilder(new ObjectMapper(), clock);

    @Test
    void build_withBody_shouldProduceSection71Envelope() throws Exception {
        record Sample(String voucherNo) {
        }
        InboundMessageProcessedEvent event = new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T-1", "SER-9", new Sample("V001"),
                Instant.parse("2026-05-19T06:30:00Z"));

        String json = builder.build(event);

        var node = new ObjectMapper().readTree(json);
        assertThat(node.get("code").asText()).isEqualTo("200");
        assertThat(node.get("message").asText()).isEqualTo("成功");
        assertThat(node.get("traceId").asText()).isEqualTo("SER-9");
        assertThat(node.get("data").get("voucherNo").asText()).isEqualTo("V001");
        assertThat(node.get("timestamp").asText()).startsWith("2026-05-19T14:30:00");
    }

    @Test
    void build_nullBody_shouldProduceEnvelopeWithNullData() throws Exception {
        InboundMessageProcessedEvent event = new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T-1", "SER-9", null,
                Instant.parse("2026-05-19T06:30:00Z"));

        String json = builder.build(event);

        var node = new ObjectMapper().readTree(json);
        assertThat(node.get("code").asText()).isEqualTo("200");
        assertThat(node.get("message").asText()).isEqualTo("成功");
        assertThat(node.get("traceId").asText()).isEqualTo("SER-9");
        assertThat(node.get("data").isNull()).isTrue();
    }
}
