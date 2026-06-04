package com.puchain.fep.web.requeststate;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 请求生命周期 inbound 结果写点：监听 {@link InboundMessageProcessedEvent}，把对应
 * outbound 请求行从 SENT 推进到 {@link RequestStateLifecycle#RESULT_RECEIVED} 终态
 * （S2 request-state tracking）。
 *
 * <p>设计镜像 {@code CallbackInboundListener}：{@code @EventListener} 同步、随 dispatcher
 * 事务边界提交/回滚，类型无关。correlation key = event 的 8 位业务 {@code transitionNo}，
 * 经 {@link RequestStateService#markResultReceived} 内部
 * {@link TransitionNoNormalizer#canonical(String)} 归一后比对。</p>
 *
 * <p><b>未匹配语义</b>：{@link RequestStateService#markResultReceived} 对不存在的 correlation
 * key 返回 {@code false}（不抛）——inbound 结果可能对应非本 FEP 发起的请求，或 correlation 行
 * 已被清理，属正常 unmatched。此时仅 {@code debug} 记录，不影响 dispatcher 主链。metric 计数
 * 留 T5 统一接入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RequestStateInboundListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(RequestStateInboundListener.class);

    private final RequestStateService service;

    /**
     * Spring 构造器注入。
     *
     * @param service request-state 单写者状态机 Service，非空
     */
    public RequestStateInboundListener(final RequestStateService service) {
        this.service = service;
    }

    /**
     * 监听 inbound 已处理事件，匹配并标记对应请求行结果已返回。
     *
     * @param event inbound 已处理事件，非空
     */
    @EventListener
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by "
                    + "LogSanitizer.sanitize (find-sec-bugs cannot detect "
                    + "user-defined sanitizer)")
    public void onProcessed(final InboundMessageProcessedEvent event) {
        final String inboundTransitionNo = event.transitionNo();
        final boolean matched = service.markResultReceived(
                inboundTransitionNo, event.serialNo(), inboundTransitionNo);
        if (!matched) {
            LOG.debug("request_state unmatched for inbound result "
                            + "transitionNo={} serialNo={}",
                    LogSanitizer.sanitize(inboundTransitionNo),
                    LogSanitizer.sanitize(event.serialNo()));
        }
    }
}
