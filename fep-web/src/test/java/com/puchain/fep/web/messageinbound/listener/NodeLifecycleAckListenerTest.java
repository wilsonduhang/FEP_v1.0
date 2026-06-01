package com.puchain.fep.web.messageinbound.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.LoginResponse9007;
import com.puchain.fep.processor.body.common.LogoutResponse9009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P4-MSG-L T2 — {@link NodeLifecycleAckListener} 单元测试（验收 criteria 6/7/8）。
 *
 * <p>listener 唯一可观测效果是 SLF4J 日志 marker（{@code NodeStateCache} hook 仍是占位注释），
 * 故用 Logback {@link ListAppender} 捕获 {@code NodeLifecycleAckListener} logger 输出断言：</p>
 * <ul>
 *   <li>9007 → INFO {@code [NODE_LOGIN_ACK]} 含 status（criteria 6）</li>
 *   <li>9009 → INFO {@code [NODE_LOGOUT_ACK]} 含 status（criteria 7）</li>
 *   <li>非 9007/9009（如 2101）→ 无 NODE marker 日志，早返回不干扰其他 listener（criteria 8）</li>
 *   <li>9007 body 为 null → WARN 降级（边界覆盖）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class NodeLifecycleAckListenerTest {

    private static final String TRANSITION_NO = "00000007";
    private static final String SERIAL_NO = "20260421100000000099";

    private NodeLifecycleAckListener listener;
    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger logbackLogger;

    @BeforeEach
    void setUp() {
        listener = new NodeLifecycleAckListener();
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logbackLogger = context.getLogger(NodeLifecycleAckListener.class);
        appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logbackLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void handle_9007_logsLoginAckWithStatus() {
        final LoginResponse9007 body = new LoginResponse9007();
        body.setStatus("01");

        listener.handle(event(MessageType.MSG_9007, body));

        assertThat(appender.list)
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.getLevel()).isEqualTo(Level.INFO);
                    assertThat(e.getFormattedMessage())
                            .contains("[NODE_LOGIN_ACK]")
                            .contains("01")
                            .contains(TRANSITION_NO);
                });
    }

    @Test
    void handle_9009_logsLogoutAckWithStatus() {
        final LogoutResponse9009 body = new LogoutResponse9009();
        body.setStatus("01");

        listener.handle(event(MessageType.MSG_9009, body));

        assertThat(appender.list)
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.getLevel()).isEqualTo(Level.INFO);
                    assertThat(e.getFormattedMessage())
                            .contains("[NODE_LOGOUT_ACK]")
                            .contains("01");
                });
    }

    @Test
    void handle_nonNodeLifecycleType_earlyReturnsWithoutLogging() {
        // 2101 不属节点登录登出回执 — listener 必须早返回，不发任何 NODE marker 日志
        listener.handle(event(MessageType.MSG_2101, null));

        assertThat(appender.list).isEmpty();
    }

    @Test
    void handle_9007_nullBody_logsWarnDowngrade() {
        listener.handle(event(MessageType.MSG_9007, null));

        assertThat(appender.list)
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.getLevel()).isEqualTo(Level.WARN);
                    assertThat(e.getFormattedMessage()).contains("[NODE_LOGIN_ACK]");
                });
    }

    private static InboundMessageProcessedEvent event(final MessageType type, final Object body) {
        return new InboundMessageProcessedEvent(type, TRANSITION_NO, SERIAL_NO, body, Instant.EPOCH);
    }
}
