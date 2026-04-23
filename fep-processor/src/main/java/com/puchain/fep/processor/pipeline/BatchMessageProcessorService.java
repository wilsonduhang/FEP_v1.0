package com.puchain.fep.processor.pipeline;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.IllegalMessageStateException;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.JAXB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 批量模式业务流水线（PRD §4.7 模式 3 信息发送 / 批量场景）。
 *
 * <p>对齐 {@link SyncMessageProcessorService} 的 save-before-transition
 * 模式：先 {@link MessageProcessStore#save(MessageProcessRecord)} 初始化
 * 记录（状态 RECEIVED），再通过 {@link MessageStateMachine#transition} 推进
 * (VALIDATED → PROCESSING → COMPLETED | FAILED)。部分成功不新增
 * {@code PARTIAL_SUCCESS} 状态，整体由 {@code failedCount == 0} 决策；失败
 * 明细完整保留在 {@link BatchResult#errors()}。</p>
 *
 * <p><b>FAILED 语义取舍</b>：本实现用 {@code stateMachine.transition(recordId, FAILED)}
 * 推进 FAILED 态，<b>不调</b> {@code stateMachine.failWith(...)}；因此
 * {@code message_process_record.error_code / error_message} 保持 {@code null}，
 * 错误明细完整保留在返回的 {@link BatchResult#errors()}。批量部分失败无法用
 * 单一 {@code FepErrorCode} 表达（N 条 record 可能 N 种错误），强行 {@code failWith}
 * 会丢信息。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class BatchMessageProcessorService {

    private static final Logger log = LoggerFactory.getLogger(BatchMessageProcessorService.class);

    private final XsdValidator xsdValidator;
    private final MessageStateMachine stateMachine;
    private final MessageProcessStore store;
    private final BatchPayloadAdapter adapter;

    /**
     * 构造器注入 4 项业务依赖。
     *
     * @param xsdValidator XSD 校验器
     * @param stateMachine 状态机（5 态）
     * @param store        持久化端口
     * @param adapter      8KB 分拆适配
     * @throws NullPointerException 任一参数为 {@code null}
     */
    public BatchMessageProcessorService(
            final XsdValidator xsdValidator,
            final MessageStateMachine stateMachine,
            final MessageProcessStore store,
            final BatchPayloadAdapter adapter) {
        this.xsdValidator = Objects.requireNonNull(xsdValidator, "xsdValidator");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.store = Objects.requireNonNull(store, "store");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    /**
     * 批量处理一条 {@link CfxMessage}（内含多条业务记录）。
     *
     * @param msg 批量报文；{@code null} 返回 {@link BatchResult#empty()}
     * @return 处理结果（success/failed counts + errors）
     */
    public BatchResult process(final CfxMessage msg) {
        if (msg == null) {
            return BatchResult.empty();
        }

        final MessageType type = extractTypeHint(msg);
        if (type == null) {
            // msgNo 未注册或 head 缺失：无法建 RECEIVED 记录，批量直接空返回。
            return BatchResult.empty();
        }
        final List<String> records = extractRecords(msg);
        if (records.isEmpty()) {
            return BatchResult.empty();
        }

        // ── Step 1: save-before-transition（对齐 SyncMessageProcessorService.process:116-119）
        //   无 transitionNo 来源时用 IdGenerator.uuid32() 占位（批量模式每次 process 独立 record）。
        final String recordId = IdGenerator.uuid32();
        final String transitionNo = msg.getHead() != null && msg.getHead().getMsgId() != null
                ? msg.getHead().getMsgId()
                : IdGenerator.uuid32();
        final Instant now = Instant.now();
        store.save(MessageProcessRecord.initial(recordId, type, transitionNo, now));
        log.debug("batch start recordId={} msgNo={} transitionNo={} count={}",
                LogSanitizer.sanitize(recordId),
                type.msgNo(),
                LogSanitizer.sanitize(transitionNo),
                records.size());

        // ── Step 2: 逐条校验 + 可能分拆（adapter.needsSplit/split 契约）
        final List<BatchResult.BatchError> errors = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < records.size(); i++) {
            final String record = records.get(i);
            // 超过 8KB 时走 adapter 分拆路径；split 结果不做额外校验，仅记录分拆事件。
            if (adapter.needsSplit(record)) {
                adapter.split(record);
                log.debug("batch record[{}] triggered split (size>8KB)", i);
            }
            final ValidationResult vr = xsdValidator.validate(
                    type, record.getBytes(StandardCharsets.UTF_8));
            if (vr.valid()) {
                success++;
            } else {
                errors.add(new BatchResult.BatchError(i, firstError(vr)));
            }
        }
        final int failed = records.size() - success;

        // ── Step 3: 状态机流转 RECEIVED → VALIDATED → PROCESSING → COMPLETED/FAILED
        //   FAILED 走 transition（非 failWith），error_code/error_message 保持 null；
        //   批量部分失败明细完整保留在 BatchResult.errors（批量场景有意设计取舍）。
        try {
            stateMachine.transition(recordId, MessageProcessStatus.VALIDATED);
            stateMachine.transition(recordId, MessageProcessStatus.PROCESSING);
            stateMachine.transition(recordId,
                    failed == 0 ? MessageProcessStatus.COMPLETED : MessageProcessStatus.FAILED);
        } catch (IllegalMessageStateException e) {
            log.warn("batch state transition rejected recordId={} reason={}",
                    LogSanitizer.sanitize(recordId), LogSanitizer.sanitize(e.getMessage()));
            // 状态机拒绝 → 本次批量整体失败（即使 failed == 0）。
            return new BatchResult(records.size(), 0, records.size(),
                    List.of(new BatchResult.BatchError(0,
                            "state transition rejected: " + e.getMessage())));
        }

        log.debug("batch completed recordId={} success={} failed={}",
                LogSanitizer.sanitize(recordId), success, failed);
        return new BatchResult(records.size(), success, failed, errors);
    }

    /**
     * 从 {@link CfxMessage} 提取批量记录（每条 body marshal 为独立 XML fragment）。
     *
     * <p>v1d 修正：用 {@link JAXB#marshal(Object, java.io.Writer)} 静态 API 将每个
     * body POJO 序列化为 XML 字符串（非 {@code Object::toString} — 后者返回类名 +
     * hashcode 非 XML，XSD 校验必挂）。JAXB static API 会为每个 body 自动构造
     * context 并 marshal 根元素（POJO 上的 {@code @XmlRootElement} 决定根名）。</p>
     *
     * <p>约定：{@link CfxMessage#getBodies()} 返回的 {@code List<Object>} 中每个
     * 非 null 元素即为一条 batch record（P1b-DEFECT-001 修复后 MsgContainer 支持 List）。</p>
     *
     * @param msg 批量报文
     * @return XML fragment 列表；{@code msg} 或 body list 为空返回空 list
     */
    private List<String> extractRecords(final CfxMessage msg) {
        if (msg == null || msg.getBodies() == null) {
            return List.of();
        }
        return msg.getBodies().stream()
                .filter(Objects::nonNull)
                .map(body -> {
                    StringWriter sw = new StringWriter();
                    JAXB.marshal(body, sw);
                    return sw.toString();
                })
                .toList();
    }

    /**
     * 从 {@link CfxMessage} head 反查 {@link MessageType} 用于 XSD 校验。
     *
     * @param msg 批量报文
     * @return 查到的 MessageType；{@code head} 缺失或 msgNo 未注册返回 {@code null}
     */
    private MessageType extractTypeHint(final CfxMessage msg) {
        if (msg == null || msg.getHead() == null) {
            return null;
        }
        final String msgNo = msg.getHead().getMsgNo();
        return MessageType.byMsgNo(msgNo).orElse(null);
    }

    private static String firstError(final ValidationResult vr) {
        return vr.errors().isEmpty() ? "unknown error" : vr.errors().get(0);
    }
}
