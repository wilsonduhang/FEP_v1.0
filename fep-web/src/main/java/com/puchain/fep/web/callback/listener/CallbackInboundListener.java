package com.puchain.fep.web.callback.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.callback.service.CallbackEnqueueService;
import com.puchain.fep.web.callback.service.CallbackTargetResolver;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 接口模式回调入口：监听 {@link InboundMessageProcessedEvent}（与 2101/
 * reconciliation listener 同级，dispatcher tx 内同步），类型无关，委派
 * resolver + enqueueService。无匹配静默跳过（数仓模式机构无配置）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackInboundListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(CallbackInboundListener.class);

    private final CallbackTargetResolver resolver;
    private final CallbackEnqueueService enqueueService;

    /**
     * @param resolver       回调目标解析器，非空
     * @param enqueueService 回调入队服务，非空
     */
    public CallbackInboundListener(final CallbackTargetResolver resolver,
                                   final CallbackEnqueueService enqueueService) {
        this.resolver = resolver;
        this.enqueueService = enqueueService;
    }

    /**
     * 监听 inbound 已处理事件，fan-out 入队回调。
     *
     * @param event inbound 已处理事件，非空
     */
    @EventListener
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "all log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    public void onProcessed(final InboundMessageProcessedEvent event) {
        final String msgNo = event.type().msgNo();
        final List<SubOutputInterface> targets = resolver.resolve(msgNo);
        if (targets.isEmpty()) {
            return;
        }
        for (final SubOutputInterface target : targets) {
            enqueueService.enqueue(target, event);
        }
        LOG.info("callback enqueued msgNo={} serialNo={} fanOut={}",
                LogSanitizer.sanitize(msgNo),
                LogSanitizer.sanitize(event.serialNo()),
                targets.size());
    }
}
