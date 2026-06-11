package com.puchain.fep.processor.pipeline;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.xml.JaxbContextCache;
import com.puchain.fep.processor.state.IllegalMessageStateException;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.BusinessRuleValidator;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import javax.xml.namespace.QName;
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
 * <p>每条记录依次过 XSD 结构关与业务规则关（镜像 {@link SyncMessageProcessorService}
 * 两关模式）；任一关失败该条计入 {@link BatchResult#errors()}。</p>
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
    private final BusinessRuleValidator businessRuleValidator;
    private final MessageStateMachine stateMachine;
    private final MessageProcessStore store;
    private final BatchPayloadAdapter adapter;
    private final OutboundWireShapeDispatcher wireShapeDispatcher;

    /**
     * 构造器注入 6 项业务依赖。
     *
     * <p>P5 T3：新增 {@link OutboundWireShapeDispatcher} 用于 {@link #wrapBodyInCfx}
     * 按 wire-shape 派发 head 元素名（修正 hardcoded {@code "RealHead" + msgNo}
     * 仅 1/8 报文正确的缺陷）。</p>
     *
     * @param xsdValidator          XSD 校验器
     * @param businessRuleValidator 业务规则校验器（XSD 之后的第二道关）
     * @param stateMachine          状态机（5 态）
     * @param store                 持久化端口
     * @param adapter               8KB 分拆适配
     * @param wireShapeDispatcher   wire-shape 路由（决定 head 元素名按 16 上行报文 dispatch）
     * @throws NullPointerException 任一参数为 {@code null}
     */
    public BatchMessageProcessorService(
            final XsdValidator xsdValidator,
            final BusinessRuleValidator businessRuleValidator,
            final MessageStateMachine stateMachine,
            final MessageProcessStore store,
            final BatchPayloadAdapter adapter,
            final OutboundWireShapeDispatcher wireShapeDispatcher) {
        this.xsdValidator = Objects.requireNonNull(xsdValidator, "xsdValidator");
        this.businessRuleValidator =
                Objects.requireNonNull(businessRuleValidator, "businessRuleValidator");
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.store = Objects.requireNonNull(store, "store");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.wireShapeDispatcher = Objects.requireNonNull(wireShapeDispatcher, "wireShapeDispatcher");
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
            final byte[] recordBytes = record.getBytes(StandardCharsets.UTF_8);
            final ValidationResult vr = xsdValidator.validate(type, recordBytes);
            if (!vr.valid()) {
                errors.add(new BatchResult.BatchError(i, firstError(vr)));
                continue;
            }
            // 第二关：业务规则（XSD 通过保证 well-formed，ValidationException 不可达；
            // 违规文案与 XSD 错误同入 BatchResult.errors，record 级 error_code 保持 null）
            final ValidationResult br = businessRuleValidator.validate(type, recordBytes);
            if (br.valid()) {
                success++;
            } else {
                errors.add(new BatchResult.BatchError(i, firstError(br)));
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
     * 从 {@link CfxMessage} 提取批量记录（每条 body 包回完整 CFX 壳体 marshal 为 XML）。
     *
     * <p>T5 修正（T4 review 捕获）：各报文 XSD 的 root 固定为 {@code <CFX>}，
     * 且 MSG 内要求 wire-shape head 元素 ({@code <RealHead{msgNo}>} 或
     * {@code <BatchHead{msgNo}>}) + {@code <body>} 两个子元素（参考
     * {@code 3005.xsd:27-43}）。若单独 marshal body 得 fragment 根（如
     * {@code <qyAccQuery3005>}），{@code XsdValidator.validate} 必报
     * {@code cvc-elt.1.a: 找不到元素 'xxx' 的声明}，COMPLETED 路径永不可达。
     * 本方法通过 {@link #wrapBodyInCfx} 把每条 body 包回
     * {@code <CFX><HEAD/><MSG><{wireHead}/><body/></MSG></CFX>} 完整壳体再 marshal，
     * 解决 fragment/root 不匹配且 MSG 缺 head 的双重问题。
     * P5 T3：head 元素名由 {@link OutboundWireShapeDispatcher} 按 16 上行报文派发，
     * inbound-only msgNo 走 legacy 路径。</p>
     *
     * <p>约定：{@link CfxMessage#getBodies()} 返回的 {@code List<Object>} 中每个
     * 非 null 元素即为一条 batch record（P1b-DEFECT-001 修复后 MsgContainer 支持 List）。</p>
     *
     * @param msg 批量报文
     * @return 完整 CFX XML 列表；{@code msg} 或 body list 为空返回空 list
     */
    private List<String> extractRecords(final CfxMessage msg) {
        if (msg == null || msg.getBodies() == null) {
            return List.of();
        }
        final CommonHead head = msg.getHead();
        final String msgNo = head != null ? head.getMsgNo() : null;
        return msg.getBodies().stream()
                .filter(Objects::nonNull)
                .map(body -> wrapBodyInCfx(head, msgNo, body))
                .toList();
    }

    /**
     * 将单条 body 包回完整 CFX 壳体
     * （{@code <CFX><HEAD/><MSG><{wireHead}/><body/></MSG></CFX>}）。
     *
     * <p>XSD root 固定为 CFX，且 MSG 要求 {@code <RealHead{msgNo}>} 或 {@code <BatchHead{msgNo}>}
     * + {@code <body>} 两子元素。本方法：
     * <ol>
     *   <li>构造 {@link CfxMessage} 复用 head；</li>
     *   <li>派生默认 {@link RequestBusinessHead}（SendOrgCode=head.srcNode，
     *       EntrustDate=head.workDate，TransitionNo=head.msgId 后 8 位数字），
     *       包装为 {@link JAXBElement} 动态指定 QName 为按 {@link OutboundWireShapeDispatcher}
     *       决定的 wire-shape head 元素名（16 上行报文：{@code RealHead3009} 或
     *       {@code BatchHead{msgNo}}），其余 inbound-only msgNo 走 legacy
     *       {@code "RealHead" + msgNo} 路径；</li>
     *   <li>把 head JAXBElement 和 body 按序放入 {@code msgContainer.contents}；</li>
     *   <li>用 {@link JaxbContextCache#getForBody(Class)} 拿缓存的 {@link JAXBContext}
     *       （注册 {@code CfxMessage} + {@code RequestBusinessHead} + {@code body.getClass()}
     *       避免 lax 降级）；</li>
     *   <li>marshal 输出完整 CFX XML。</li>
     * </ol>
     *
     * <p><b>P5 T3 修复</b>：历史实现 hardcoded {@code "RealHead" + msgNo} 仅 3009 正确
     * （16 上行报文中其余 15 个为 {@code BatchHead{msgNo}}）。改用
     * {@link OutboundWireShapeDispatcher#describeFor} 决定 head 元素名。
     * 对未登记 outbound msgNo（如 2101 inbound-only 等），保留 legacy
     * 行为避免 inbound 链路回归。</p>
     *
     * <p><b>默认 head 取值策略</b>：service 层无法从 {@link CfxMessage} 模型
     * 反推精确 head 值（模型不含 RealHead/BatchHead 字段），故从 HEAD 衍生合理默认值以
     * 满足 RequestHead / ResponseHead XSD 结构。业务生产路径若需精确 head，由上游
     * ConverterPipeline 在 CfxMessage 构造时通过扩展模型预先注入。</p>
     *
     * <p><b>访问性</b>：package-private 以便 {@code WrapBodyInCfxFixTest} 直接断言
     * wire-shape head 元素名 — 内部方法，无外部调用方。</p>
     *
     * @param head  共享 head（来自原 batch msg）
     * @param msgNo 报文号（决定 wire-shape head 元素名）
     * @param body  单条 body POJO
     * @return 完整 CFX XML 字符串
     * @throws IllegalStateException JAXB 构建 context / marshal 失败
     */
    String wrapBodyInCfx(final CommonHead head, final String msgNo, final Object body) {
        final CfxMessage wrapper = new CfxMessage();
        wrapper.setHead(head);
        final CfxMessage.MsgContainer container = new CfxMessage.MsgContainer();

        // 派生默认 head（SendOrgCode 14 位 / EntrustDate 8 位 YYYYMMDD /
        // TransitionNo 8 位数字）。若 head 字段缺失或不满足长度约束，service 不
        // 强行补齐，RequestBusinessHead setter 的入参 null 允许通过。
        final RequestBusinessHead realHead = deriveRealHead(head);

        // P5 T3 fix: wire-shape head 元素名按 dispatcher 决定。16 上行报文使用
        // dispatcher.describeFor 派发（3009→RealHead3009，其余 15→BatchHead{msgNo}）；
        // 未登记 msgNo（如 2101 inbound-only 等）走 legacy 路径不变以避免回归。
        final String headElementName = resolveHeadElementName(msgNo);
        final JAXBElement<RequestBusinessHead> headElement = new JAXBElement<>(
                new QName(headElementName), RequestBusinessHead.class, realHead);

        container.getContents().add(headElement);
        container.getContents().add(body);
        wrapper.setMsgContainer(container);

        final StringWriter sw = new StringWriter();
        try {
            final JAXBContext ctx = JaxbContextCache.getForBody(body.getClass());
            final Marshaller marshaller = ctx.createMarshaller();
            marshaller.marshal(wrapper, sw);
        } catch (JAXBException e) {
            throw new IllegalStateException(
                    "Failed to marshal CFX wrapper for body " + body.getClass().getName(), e);
        }
        return sw.toString();
    }

    /**
     * 解析 wire-shape head 元素名：已登记上行报文走 dispatcher，其余走 legacy。
     *
     * <p>P5 T3 — 历史 dispatcher 仅登记 16 上行报文（现已 append-only 持续演进，实数见
     * {@link OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT}）。
     * 未登记 outbound msgNo（如 2101 inbound-only 等）直接调用
     * {@code dispatcher.describeFor} 会抛 OUTBOUND_5108 误伤已有 inbound IT。用
     * {@link OutboundWireShapeDispatcher#isRegisteredOutboundMsgNo} 先判断，未登记走
     * legacy {@code "RealHead" + msgNo} 路径保持向后兼容。</p>
     *
     * @param msgNo 4 位数字报文号；{@code null} 退化为 {@code "RealHead"}（与 legacy 行为一致）
     * @return wire-shape head 元素名
     */
    private String resolveHeadElementName(final String msgNo) {
        if (wireShapeDispatcher.isRegisteredOutboundMsgNo(msgNo)) {
            return wireShapeDispatcher.describeFor(msgNo).headElementName();
        }
        // Legacy fallback for unregistered outbound msgNo (e.g. 2101).
        return "RealHead" + (msgNo != null ? msgNo : "");
    }

    /**
     * 从 HEAD 派生默认 {@link RequestBusinessHead}。
     *
     * <p>SendOrgCode 取 {@code head.srcNode}（14 位金融机构代码）；
     * EntrustDate 取 {@code head.workDate}（YYYYMMDD）；
     * TransitionNo 取 {@code head.msgId} 后 8 位数字（msgId 为 20 位 YYYYMMDDHHMMSSxxxxxx）。
     * 任一字段若不满足 RequestBusinessHead setter 的校验（非 null 下长度/格式），
     * setter 会抛 {@link IllegalArgumentException}，本方法不做兜底，让故障显式暴露。</p>
     *
     * @param head HEAD，{@code null} 返回空 RealHead
     * @return 派生 RealHead
     */
    private RequestBusinessHead deriveRealHead(final CommonHead head) {
        final RequestBusinessHead rh = new RequestBusinessHead();
        if (head == null) {
            return rh;
        }
        rh.setSendOrgCode(head.getSrcNode());
        rh.setEntrustDate(head.getWorkDate());
        final String msgId = head.getMsgId();
        if (msgId != null && msgId.length() >= 8) {
            final String candidate = msgId.substring(msgId.length() - 8);
            // setter 仅接受 8 位数字；非数字 msgId 尾段回退到 null（setter 允许）。
            if (candidate.chars().allMatch(Character::isDigit)) {
                rh.setTransitionNo(candidate);
            }
        }
        return rh;
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
