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
 * PRD v1.3 §4.7 模式1 同步流水线编排器：接收报文 → XSD 校验 → 业务规则校验 → 状态流转 → 返回最终记录。
 *
 * <p>本类只负责"校验 + 状态管理"核心职责，不承担 TLQ 发送或 XML 编组/解组
 * （由 P1b {@code MessageEncoder/Decoder} 或上层业务编排完成）。这样既保持职责边界
 * 清晰，也便于 P2b/P2c 扩展异步/通用模式时共用同一状态机。</p>
 *
 * <p>校验分两关：先 XSD 结构校验（{@link XsdValidator}，失败 {@code PROC_8501}），
 * 再业务语义/跨字段校验（{@link BusinessRuleValidator}，失败 {@code PROC_8507}；
 * 未配置规则的报文类型默认放行）。</p>
 *
 * <p>流水线：<br>
 * {@code save(RECEIVED) → xsdValidate → [invalid → FAILED(PROC_8501)] |
 * [valid → businessRuleValidate → [invalid → FAILED(PROC_8507)] |
 * [valid → VALIDATED → PROCESSING → COMPLETED]]}</p>
 *
 * <p>所有 {@code transitionNo} 与错误消息在写入日志前经
 * {@link LogSanitizer#sanitize} 清洗，防御 CRLF 日志注入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class SyncMessageProcessorService {

    private static final Logger log = LoggerFactory.getLogger(SyncMessageProcessorService.class);

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
    public SyncMessageProcessorService(final XsdValidator validator,
                                       final BusinessRuleValidator businessRuleValidator,
                                       final MessageStateMachine stateMachine,
                                       final MessageProcessStore store) {
        this.validator = validator;
        this.businessRuleValidator = businessRuleValidator;
        this.stateMachine = stateMachine;
        this.store = store;
    }

    /**
     * 处理外发（outbound）报文。流水线同 {@link #process(MessageType, String, byte[], String)}，
     * 方向标签为 {@code "OUT"}。
     *
     * @param type         报文类型，非空；必须是 11 种同步报文之一
     * @param transitionNo 业务流水号，非空，长度 ≤ 30
     * @param xml          UTF-8 编码的 XML payload，非空，≤ 10MB
     * @return 终态记录（{@link MessageProcessStatus#COMPLETED}
     *         或 {@link MessageProcessStatus#FAILED}）
     * @throws IllegalArgumentException      {@code type} / {@code transitionNo} / {@code xml} 有一项为 {@code null}
     * @throws IllegalStateException         {@code transitionNo} 已存在于 store（防重放）
     * @throws UnsupportedOperationException {@code type} 不在 11 种同步报文白名单内
     */
    public MessageProcessRecord processOutbound(final MessageType type,
                                                final String transitionNo,
                                                final byte[] xml) {
        return process(type, transitionNo, xml, "OUT");
    }

    /**
     * 处理接收（inbound）报文。语义与异常约定同
     * {@link #processOutbound(MessageType, String, byte[])}，方向标签为 {@code "IN"}。
     *
     * @param type         报文类型，非空
     * @param transitionNo 业务流水号，非空
     * @param xml          UTF-8 编码的 XML payload，非空
     * @return 终态记录
     * @throws IllegalArgumentException      任一必填参数为 {@code null}
     * @throws IllegalStateException         {@code transitionNo} 重复
     * @throws UnsupportedOperationException 报文类型不受支持
     */
    public MessageProcessRecord processInbound(final MessageType type,
                                               final String transitionNo,
                                               final byte[] xml) {
        return process(type, transitionNo, xml, "IN");
    }

    private MessageProcessRecord process(final MessageType type,
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

        log.info("[{}] processing msg={} transitionNo={} recordId={}",
                direction, type.msgNo(), LogSanitizer.sanitize(transitionNo), recordId);

        ValidationResult vr = validator.validate(type, xml);
        if (!vr.valid()) {
            String firstError = vr.errors().isEmpty() ? "unknown" : vr.errors().get(0);
            log.warn("[{}] xsd validation failed msg={} transitionNo={} firstError={}",
                    direction, type.msgNo(),
                    LogSanitizer.sanitize(transitionNo),
                    LogSanitizer.sanitize(firstError));
            return stateMachine.failWith(saved, FepErrorCode.PROC_8501, firstError);
        }

        ValidationResult br = businessRuleValidator.validate(type, xml);
        if (!br.valid()) {
            String firstError = br.errors().isEmpty() ? "unknown" : br.errors().get(0);
            log.warn("[{}] business rule validation failed msg={} transitionNo={} firstError={}",
                    direction, type.msgNo(),
                    LogSanitizer.sanitize(transitionNo),
                    LogSanitizer.sanitize(firstError));
            return stateMachine.failWith(saved, FepErrorCode.PROC_8507, firstError);
        }

        MessageProcessRecord validated = stateMachine.transition(saved, MessageProcessStatus.VALIDATED);
        MessageProcessRecord processing = stateMachine.transition(validated, MessageProcessStatus.PROCESSING);
        MessageProcessRecord completed = stateMachine.transition(processing, MessageProcessStatus.COMPLETED);

        log.info("[{}] msg={} transitionNo={} recordId={} completed",
                direction, type.msgNo(), LogSanitizer.sanitize(transitionNo), recordId);
        return completed;
    }
}
