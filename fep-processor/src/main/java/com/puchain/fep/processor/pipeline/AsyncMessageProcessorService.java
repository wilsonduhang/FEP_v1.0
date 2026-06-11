package com.puchain.fep.processor.pipeline;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.BusinessRuleValidator;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * PRD v1.3 §4.7 模式2/5 异步流水线编排器：接收报文 → XSD 校验 → 业务规则校验 →
 * 状态流转至 PROCESSING → 返回记录。
 *
 * <p>与 {@link SyncMessageProcessorService} 的核心区别：同步模式在单次调用中走完
 * {@code RECEIVED → VALIDATED → PROCESSING → COMPLETED}；异步模式分两阶段：</p>
 * <ol>
 *   <li>{@link #processAsyncInbound} / {@link #processAsyncOutbound}：走到
 *       {@code PROCESSING} 停止，调用方基于返回记录生成 9120 ACK</li>
 *   <li>{@link #completeWithResponse}：收到异步应答（如 3002/3004/3006）后，
 *       对应答 XML 依次过 XSD 校验 → 业务规则校验，再将原记录从
 *       {@code PROCESSING → COMPLETED}</li>
 * </ol>
 *
 * <p>所有 {@code transitionNo} 与错误消息在写入日志前经
 * {@link LogSanitizer#sanitize} 清洗，防御 CRLF 日志注入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class AsyncMessageProcessorService {

    private static final Logger log = LoggerFactory.getLogger(AsyncMessageProcessorService.class);

    private final XsdValidator validator;
    private final BusinessRuleValidator businessRuleValidator;
    private final MessageStateMachine stateMachine;
    private final MessageProcessStore store;

    /**
     * Spring 构造注入四个协作者。所有依赖均为 {@code final}，不参与可变状态共享。
     *
     * @param validator             XSD 结构校验器，非空
     * @param businessRuleValidator 业务规则校验器（XSD 之后的第二道关），非空
     * @param stateMachine          报文级状态机，非空
     * @param store                 报文处理记录存储端口，非空
     */
    public AsyncMessageProcessorService(final XsdValidator validator,
                                        final BusinessRuleValidator businessRuleValidator,
                                        final MessageStateMachine stateMachine,
                                        final MessageProcessStore store) {
        this.validator = validator;
        this.businessRuleValidator = businessRuleValidator;
        this.stateMachine = stateMachine;
        this.store = store;
    }

    /**
     * 模式2/5 接收（inbound）异步请求报文。校验通过后状态停留在
     * {@link MessageProcessStatus#PROCESSING}，调用方使用返回记录生成 9120 ACK。
     *
     * <p>状态流转：{@code RECEIVED → VALIDATED → PROCESSING}</p>
     *
     * @param type         报文类型，非空
     * @param transitionNo 业务流水号，非空，长度 ≤ 30
     * @param xml          UTF-8 编码的 XML payload，非空
     * @return PROCESSING 态记录（校验失败时为 FAILED 态记录）
     * @throws IllegalArgumentException 任一必填参数为 {@code null}
     * @throws IllegalStateException    {@code transitionNo} 重复
     */
    public MessageProcessRecord processAsyncInbound(final MessageType type,
                                                    final String transitionNo,
                                                    final byte[] xml) {
        return processAsync(type, transitionNo, xml, "IN");
    }

    /**
     * 模式2/5 外发（outbound）异步请求报文。本地校验通过后状态停留在
     * {@link MessageProcessStatus#PROCESSING}，调用方据此经 TLQ 发送报文并等待 9120 ACK。
     *
     * <p>状态流转：{@code RECEIVED → VALIDATED → PROCESSING}</p>
     *
     * @param type         报文类型，非空
     * @param transitionNo 业务流水号，非空，长度 ≤ 30
     * @param xml          UTF-8 编码的 XML payload，非空
     * @return PROCESSING 态记录（校验失败时为 FAILED 态记录）
     * @throws IllegalArgumentException 任一必填参数为 {@code null}
     * @throws IllegalStateException    {@code transitionNo} 重复
     */
    public MessageProcessRecord processAsyncOutbound(final MessageType type,
                                                     final String transitionNo,
                                                     final byte[] xml) {
        return processAsync(type, transitionNo, xml, "OUT");
    }

    /**
     * 异步流完成：收到应答报文（如 3002/3004/3006），校验应答 XML 后
     * 将原始 PROCESSING 记录迁移至 {@link MessageProcessStatus#COMPLETED}。
     *
     * @param originalTransitionNo 原始请求的流水号，非空
     * @param responseType         应答报文类型，非空
     * @param responseXml          应答 XML payload，非空
     * @return COMPLETED 态记录（校验失败时为 FAILED 态记录）
     * @throws IllegalArgumentException 任一参数为 {@code null}，或原始流水号未找到
     * @throws IllegalStateException    原始记录不在 PROCESSING 状态
     */
    public MessageProcessRecord completeWithResponse(final String originalTransitionNo,
                                                     final MessageType responseType,
                                                     final byte[] responseXml) {
        if (originalTransitionNo == null) {
            throw new IllegalArgumentException("originalTransitionNo must not be null");
        }
        if (responseType == null) {
            throw new IllegalArgumentException("responseType must not be null");
        }
        if (responseXml == null) {
            throw new IllegalArgumentException("responseXml must not be null");
        }

        MessageProcessRecord original = store.findByTransitionNo(originalTransitionNo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "no record found for transitionNo: "
                                + LogSanitizer.sanitize(originalTransitionNo)));

        if (original.getStatus() != MessageProcessStatus.PROCESSING) {
            throw new IllegalStateException(
                    "record must be in PROCESSING state, but was "
                            + original.getStatus() + " (transitionNo="
                            + LogSanitizer.sanitize(originalTransitionNo) + ")");
        }

        log.info("[ASYNC-COMPLETE] validating response msg={} for transitionNo={}",
                responseType.msgNo(), LogSanitizer.sanitize(originalTransitionNo));

        ValidationResult vr = validator.validate(responseType, responseXml);
        if (!vr.valid()) {
            String firstError = vr.errors().isEmpty() ? "unknown" : vr.errors().get(0);
            log.warn("[ASYNC-COMPLETE] response xsd validation failed msg={} transitionNo={} "
                            + "firstError={}",
                    responseType.msgNo(),
                    LogSanitizer.sanitize(originalTransitionNo),
                    LogSanitizer.sanitize(firstError));
            return stateMachine.failWith(original, FepErrorCode.PROC_8501, firstError);
        }

        ValidationResult br = businessRuleValidator.validate(responseType, responseXml);
        if (!br.valid()) {
            String firstError = br.errors().isEmpty() ? "unknown" : br.errors().get(0);
            log.warn("[ASYNC-COMPLETE] response business rule validation failed msg={} "
                            + "transitionNo={} firstError={}",
                    responseType.msgNo(),
                    LogSanitizer.sanitize(originalTransitionNo),
                    LogSanitizer.sanitize(firstError));
            return stateMachine.failWith(original, FepErrorCode.PROC_8507, firstError);
        }

        MessageProcessRecord completed = stateMachine.transition(original,
                MessageProcessStatus.COMPLETED);

        log.info("[ASYNC-COMPLETE] msg={} transitionNo={} recordId={} completed",
                responseType.msgNo(), LogSanitizer.sanitize(originalTransitionNo),
                completed.getId());
        return completed;
    }

    private MessageProcessRecord processAsync(final MessageType type,
                                              final String transitionNo,
                                              final byte[] xml,
                                              final String direction) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (transitionNo == null) {
            throw new IllegalArgumentException("transitionNo must not be null");
        }
        if (xml == null) {
            throw new IllegalArgumentException("xml must not be null");
        }

        store.findByTransitionNo(transitionNo).ifPresent(existing -> {
            throw new IllegalStateException("duplicate transitionNo: "
                    + LogSanitizer.sanitize(transitionNo));
        });

        String recordId = IdGenerator.uuid32();
        Instant now = Instant.now();
        MessageProcessRecord saved = store.save(
                MessageProcessRecord.initial(recordId, type, transitionNo, now));

        log.info("[ASYNC-{}] processing msg={} transitionNo={} recordId={}",
                direction, type.msgNo(), LogSanitizer.sanitize(transitionNo), recordId);

        ValidationResult vr = validator.validate(type, xml);
        if (!vr.valid()) {
            String firstError = vr.errors().isEmpty() ? "unknown" : vr.errors().get(0);
            log.warn("[ASYNC-{}] xsd validation failed msg={} transitionNo={} firstError={}",
                    direction, type.msgNo(),
                    LogSanitizer.sanitize(transitionNo),
                    LogSanitizer.sanitize(firstError));
            return stateMachine.failWith(saved, FepErrorCode.PROC_8501, firstError);
        }

        ValidationResult br = businessRuleValidator.validate(type, xml);
        if (!br.valid()) {
            String firstError = br.errors().isEmpty() ? "unknown" : br.errors().get(0);
            log.warn("[ASYNC-{}] business rule validation failed msg={} transitionNo={} "
                            + "firstError={}",
                    direction, type.msgNo(),
                    LogSanitizer.sanitize(transitionNo),
                    LogSanitizer.sanitize(firstError));
            return stateMachine.failWith(saved, FepErrorCode.PROC_8507, firstError);
        }

        MessageProcessRecord validated = stateMachine.transition(saved,
                MessageProcessStatus.VALIDATED);
        MessageProcessRecord processing = stateMachine.transition(validated,
                MessageProcessStatus.PROCESSING);

        log.info("[ASYNC-{}] msg={} transitionNo={} recordId={} now PROCESSING (awaiting response)",
                direction, type.msgNo(), LogSanitizer.sanitize(transitionNo), recordId);
        return processing;
    }
}
