package com.puchain.fep.web.callback.service;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 回调入队（{@link Propagation#REQUIRES_NEW} 解耦 inbound dispatcher tx，
 * 镜像 {@code JpaOutboundMessageEnqueueService}）。幂等 key =
 * SHA-256(serialNo+interfaceId) 取前 32-hex（与 2101
 * {@code BizMessage2101InboundListener} 32-hex proven 模式一致；
 * {@code callback_queue.idempotency_key} 列 length 64 为预留 headroom）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class CallbackEnqueueService {

    private static final Logger LOG =
            LoggerFactory.getLogger(CallbackEnqueueService.class);
    private static final int KEY_HEX_LEN = 32;

    private final CallbackQueueRepository repository;
    private final CallbackEnvelopeBuilder envelopeBuilder;

    /**
     * @param repository      回调队列仓储，非空
     * @param envelopeBuilder §7.1 封套构建器，非空
     */
    public CallbackEnqueueService(final CallbackQueueRepository repository,
                                  final CallbackEnvelopeBuilder envelopeBuilder) {
        this.repository = repository;
        this.envelopeBuilder = envelopeBuilder;
    }

    /**
     * 入队一条回调（幂等；REQUIRES_NEW 独立事务）。
     *
     * @param target 解析命中的输出接口，非空
     * @param event  inbound 已处理事件，非空
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "all log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    public void enqueue(final SubOutputInterface target,
                        final InboundMessageProcessedEvent event) {
        final String key = deriveKey(event.serialNo(), target.getInterfaceId());
        if (repository.existsByIdempotencyKey(key)) {
            LOG.warn("callback dup skip key={} serialNo={}",
                    LogSanitizer.sanitize(key), LogSanitizer.sanitize(event.serialNo()));
            return;
        }
        final String payload = envelopeBuilder.build(event);
        try {
            repository.save(CallbackQueueEntity.pending(
                    key, target.getInterfaceId(), event.type().msgNo(), payload));
        } catch (final DataIntegrityViolationException dup) {
            LOG.warn("callback enqueue race dup swallowed key={}",
                    LogSanitizer.sanitize(key));
        }
    }

    private static String deriveKey(final String serialNo, final String interfaceId) {
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            final byte[] h = sha.digest(
                    (serialNo + "|" + interfaceId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h).substring(0, KEY_HEX_LEN);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 missing on JVM", e);
        }
    }
}
