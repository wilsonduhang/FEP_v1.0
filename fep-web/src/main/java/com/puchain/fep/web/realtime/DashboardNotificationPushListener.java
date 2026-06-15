package com.puchain.fep.web.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.notification.event.InAppNotificationCreatedEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Objects;

/**
 * 站内通知创建事件 → WebSocket 实时推送监听器（B-8，FR-WEB-DASH-REFRESH）。
 *
 * <p>消费 {@link InAppNotificationCreatedEvent}，向收件用户的活跃 WebSocket 会话推送
 * {@code {"type":"notification","notificationId":...}}（用户无活跃会话则由
 * {@link WebSocketSessionRegistry#sendToUser} 静默跳过——前端将经常规轮询兜底）。</p>
 *
 * <p><b>{@link TransactionalEventListener}(AFTER_COMMIT)</b>：通知必须在数据库事务
 * 提交后才推送，否则客户端收到推送后立即查 {@code listUnread} 可能撞上尚未提交的写入
 * （读不到）。{@code fallbackExecution=true} 使无事务上下文（如直接发布）时仍执行，
 * 保证健壮性。推送不抛异常回滚业务（registry 内部吞 IO 错并丢弃坏会话）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "userId passed through LogSanitizer.sanitize() prior to LOG; payload contains only server-generated notificationId")
public class DashboardNotificationPushListener {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardNotificationPushListener.class);
    private static final String PUSH_TYPE = "notification";

    private final WebSocketSessionRegistry registry;
    private final ObjectMapper objectMapper;

    /**
     * @param registry     会话注册表（Spring bean）
     * @param objectMapper JSON 序列化（Spring bean）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons (registry/mapper) stored by reference per container contract")
    public DashboardNotificationPushListener(final WebSocketSessionRegistry registry,
            final ObjectMapper objectMapper) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * 事务提交后推送通知到目标用户活跃会话。
     *
     * @param event 站内通知创建事件（非 null）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationCreated(final InAppNotificationCreatedEvent event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(
                    Map.of("type", PUSH_TYPE, "notificationId", event.notificationId()));
        } catch (final JsonProcessingException ex) {
            LOG.warn("WS notification push serialization failed userId={} reason={}",
                    LogSanitizer.sanitize(event.userId()),
                    LogSanitizer.sanitize(ex.getClass().getSimpleName()));
            return;
        }
        registry.sendToUser(event.userId(), payload);
    }
}
