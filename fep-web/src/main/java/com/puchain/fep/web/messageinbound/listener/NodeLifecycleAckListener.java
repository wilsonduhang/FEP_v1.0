package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.LoginResponse9007;
import com.puchain.fep.processor.body.common.LogoutResponse9009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 节点登录/登出回执 inbound listener（PRD v1.3 §4.5 通用 9 报文 — 9007/9009）。
 *
 * <p>处理 9007（节点登录回执）/ 9009（节点登出回执）两类节点状态应答 — log 节点状态
 * 变更，可扩展 NodeStateCache hook 维护内存节点状态（本 Plan 范围不实装 cache，留待
 * 后续节点连接管理 Plan）。</p>
 *
 * <p><b>不复用 {@link AbstractAck9120InboundListener}</b>：9007/9009 不走 9120-ack 通道，
 * 无业务幂等 + 无 record 持久化 + 无反向 ack — 简单 log + 后续 hook 即可（对照 P4-MSG-L
 * Plan §Task2 范围声明）。</p>
 *
 * <p>{@link InboundMessageProcessedEvent#type()} 返回 {@link MessageType} 枚举；本 listener
 * 顶层按 {@code MessageType.MSG_9007 / MSG_9009} 区分，非这两类 msgNo 早返回不干扰其他
 * listener（Spring {@code @EventListener} 广播给所有 bean）。所有日志参数走
 * {@link LogSanitizer#sanitize} 防御 CRLF 日志注入；与上游 9006/9008 Password 明文隔离
 * （见 {@link LoginResponse9007} / {@link LogoutResponse9009} 类 Javadoc 安全说明）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class NodeLifecycleAckListener {

    private static final Logger LOG = LoggerFactory.getLogger(NodeLifecycleAckListener.class);

    /**
     * 处理 inbound 节点登录/登出回执事件。
     *
     * <p>按 {@link InboundMessageProcessedEvent#type()} 区分 9007/9009；其他报文类型
     * 早返回（本 listener 仅订阅节点生命周期回执）。</p>
     *
     * @param event 入站处理完成事件，非空（Spring 发布保证）；{@code body} 可空
     */
    @EventListener
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "All log params wrapped via LogSanitizer.sanitize()")
    public void handle(final InboundMessageProcessedEvent event) {
        final MessageType type = event.type();
        if (type == MessageType.MSG_9007) {
            handleLoginAck(event);
        } else if (type == MessageType.MSG_9009) {
            handleLogoutAck(event);
        }
        // 非 9007/9009 早返回，不干扰其他 listener
    }

    private void handleLoginAck(final InboundMessageProcessedEvent event) {
        if (event.body() instanceof LoginResponse9007 body) {
            LOG.info("[NODE_LOGIN_ACK] 登录成功 transitionNo={} status={}",
                    LogSanitizer.sanitize(event.transitionNo()),
                    LogSanitizer.sanitize(body.getStatus()));
            // Future hook: NodeStateCache.update(status)
        } else {
            LOG.warn("[NODE_LOGIN_ACK] body 为 null 或类型不符, transitionNo={}",
                    LogSanitizer.sanitize(event.transitionNo()));
        }
    }

    private void handleLogoutAck(final InboundMessageProcessedEvent event) {
        if (event.body() instanceof LogoutResponse9009 body) {
            LOG.info("[NODE_LOGOUT_ACK] 登出成功 transitionNo={} status={}",
                    LogSanitizer.sanitize(event.transitionNo()),
                    LogSanitizer.sanitize(body.getStatus()));
            // Future hook: NodeStateCache.markLoggedOut(status)
        } else {
            LOG.warn("[NODE_LOGOUT_ACK] body 为 null 或类型不符, transitionNo={}",
                    LogSanitizer.sanitize(event.transitionNo()));
        }
    }
}
