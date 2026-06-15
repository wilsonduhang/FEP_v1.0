package com.puchain.fep.web.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.callback.notification.event.InAppNotificationCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * {@link DashboardNotificationPushListener} 单元测试：事件 → registry.sendToUser 推送载荷。
 */
@ExtendWith(MockitoExtension.class)
class DashboardNotificationPushListenerTest {

    @Mock
    private WebSocketSessionRegistry registry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DashboardNotificationPushListener listener;

    @BeforeEach
    void setUp() {
        listener = new DashboardNotificationPushListener(registry, objectMapper);
    }

    @Test
    void onNotificationCreated_pushesNotificationFrameToUser() {
        final InAppNotificationCreatedEvent event =
                new InAppNotificationCreatedEvent("user-1", "notif-abc", Instant.now());

        listener.onNotificationCreated(event);

        final ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(registry).sendToUser(eq("user-1"), payload.capture());
        assertThat(payload.getValue())
                .contains("\"type\":\"notification\"")
                .contains("\"notificationId\":\"notif-abc\"");
    }

    @Test
    void onNotificationCreated_payloadIsValidJsonWithExpectedFields() throws Exception {
        final InAppNotificationCreatedEvent event =
                new InAppNotificationCreatedEvent("user-2", "notif-xyz", Instant.now());

        listener.onNotificationCreated(event);

        final ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(registry).sendToUser(eq("user-2"), payload.capture());
        final var node = objectMapper.readTree(payload.getValue());
        assertThat(node.path("type").asText()).isEqualTo("notification");
        assertThat(node.path("notificationId").asText()).isEqualTo("notif-xyz");
    }
}
