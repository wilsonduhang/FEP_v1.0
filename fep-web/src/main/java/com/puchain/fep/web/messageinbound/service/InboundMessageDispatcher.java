package com.puchain.fep.web.messageinbound.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.xml.JaxbContextCache;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchResponse2104;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchResponse2103;
import com.puchain.fep.processor.body.batch.DataTransfer2101;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchResponse2102;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import com.puchain.fep.processor.body.supplychain.ProgressQuery3001;
import com.puchain.fep.processor.body.supplychain.ProgressQueryReturn3002;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.pipeline.SyncMessageProcessorService;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.web.messageinbound.dto.InboundMessageResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 入站报文分发器（PRD §5.3.2.13 + §1991 状态机）。
 *
 * <p>P3 Task 2 — 把"路由 + 持久化 + body 解析 + 事件发布"四件事串起来：</p>
 * <ol>
 *   <li>{@link MessageType#byMsgNo(String)} 反查报文类型 — 未注册抛
 *       {@link FepErrorCode#MSG_INBOUND_INVALID_TYPE}。</li>
 *   <li>委派 {@link SyncMessageProcessorService#processInbound} 完成 XSD 校验
 *       + RECEIVED→VALIDATED→PROCESSING→COMPLETED 状态流转。</li>
 *   <li>仅当流水线终态 {@link MessageProcessStatus#COMPLETED} 时再 unmarshal body
 *       并 publish {@link InboundMessageProcessedEvent}；FAILED 不发事件。</li>
 *   <li>整方法标 {@link Transactional}（v1a P0-Q1） — listener 同步同事务，
 *       listener 抛异常会回滚 message_process_record + 下游 reconciliation_records。</li>
 * </ol>
 *
 * <p>Body POJO 解析使用本地 JAXBContext 缓存（per body class），按 P3 Phase 2
 * + P4-MSG-B-inbound + P4-MSG-A-inbound + P4-MSG-D + P4-Plan-C + P4-MSG-J 范围登记 17 种业务 body。
 * 其他 messageType 不解析 body，事件 {@code body} 字段留 null（listener 自行降级处理）。</p>
 *
 * <p>Inbound BODY_TYPE_REGISTRY 注册项（按 msgNo 升序，{@link Map#ofEntries} 不限 entry 数）:</p>
 * <ul>
 *   <li>2101 → {@link DataTransfer2101}（HNDEMP 数据推送，P4-MSG-D T4）</li>
 *   <li>2102 → {@link DataTransferCheckBatchResponse2102}（数据报送核对回执，P4-MSG-A-inbound T1）</li>
 *   <li>2103 → {@link CompanyInfoBatchResponse2103}（企业信息批量查询回执，P4-MSG-A-inbound T1）</li>
 *   <li>2104 → {@link CompanyAuthFileBatchResponse2104}（授权书批量回执，P4-MSG-A-inbound T1）</li>
 *   <li>3001 → {@link ProgressQuery3001}（业务进展实时查询请求，P4-Plan-C T1）</li>
 *   <li>3002 → {@link ProgressQueryReturn3002}（业务进展查询回执，P4-Plan-C T1）</li>
 *   <li>3003 → {@link PzInfoQuery3003}（电子凭证融资状态查询请求，P4-Plan-C T1）</li>
 *   <li>3004 → {@link PzInfoReturn3004}（电子凭证融资状态查询回执，P4-Plan-C T1）</li>
 *   <li>3005 → {@link QyAccQuery3005}（对公账户状态查询请求，P4-Plan-C T1）</li>
 *   <li>3006 → {@link QyAccQueryReturn3006}（对公客户状态查询回执，P4-Plan-C T1）</li>
 *   <li>3007 → {@link InvoCheckQuery3007}（发票核验请求，P4-MSG-B-inbound v1）</li>
 *   <li>3008 → {@link InvoCheckReturn3008}（发票核验回执，P4-MSG-B-inbound v1）</li>
 *   <li>3107 → {@link PzCheckQuery3107}（凭证核验查询，P3 Phase 2 wiring）</li>
 *   <li>3108 → {@link PzCheckQueryReturn3108}（凭证核验查询回执，P3 Phase 2 wiring）</li>
 *   <li>3112 → {@link HxqyCreditAmt3112}（核心企业授信查询请求，银行被动接收 模式5，P4-MSG-J）</li>
 *   <li>3115 → {@link PlatPay3115}（资金清算指令，P3 Phase 2 wiring）</li>
 *   <li>3116 → {@link BankCheckDay3116}（资金日对账，P3 Phase 2 wiring）</li>
 * </ul>
 *
 * <p>所有日志参数走 {@link LogSanitizer#sanitize}，防御 CRLF 日志注入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class InboundMessageDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(InboundMessageDispatcher.class);

    /**
     * P3 Phase 2 + P4-MSG-B-inbound + P4-MSG-A-inbound + P4-MSG-D + P4-Plan-C 注册的 body POJO 反查表：
     * messageType.msgNo → body class。仅用于 dispatcher 解析 body POJO；listener
     * 各自再做安全 cast。顺序：按 msgNo 升序（2101 P4-MSG-D T4 → 2102/2103/2104 P4-MSG-A-inbound →
     * 3001/3002/3003/3004/3005/3006 P4-Plan-C T1 → 3007/3008 P4-MSG-B-inbound → 3107/3108/3115/3116 P3 Phase 2
     * + P4-MSG-J 3112）。
     *
     * <p>详细注册项见包级 Javadoc {@code <ul>} 列表（与本字段 1:1 对齐）。</p>
     *
     * <p>使用 {@link Map#ofEntries} 不限 entry 数（P4-MSG-D T0 起，从 {@link Map#of}
     * 9/10 上限 refactor — Roadmap §3 强制约束，为后续 inbound Plan
     * append-only 增长留出空间）。{@code Map.ofEntries} 与 {@code Map.of} 同样产
     * 出不可变 Map，性能与 hash 行为一致。</p>
     */
    private static final Map<String, Class<?>> BODY_TYPE_REGISTRY = Map.ofEntries(
            Map.entry(MessageType.MSG_2101.msgNo(), DataTransfer2101.class),
            Map.entry(MessageType.MSG_2102.msgNo(), DataTransferCheckBatchResponse2102.class),
            Map.entry(MessageType.MSG_2103.msgNo(), CompanyInfoBatchResponse2103.class),
            Map.entry(MessageType.MSG_2104.msgNo(), CompanyAuthFileBatchResponse2104.class),
            Map.entry(MessageType.MSG_3001.msgNo(), ProgressQuery3001.class),
            Map.entry(MessageType.MSG_3002.msgNo(), ProgressQueryReturn3002.class),
            Map.entry(MessageType.MSG_3003.msgNo(), PzInfoQuery3003.class),
            Map.entry(MessageType.MSG_3004.msgNo(), PzInfoReturn3004.class),
            Map.entry(MessageType.MSG_3005.msgNo(), QyAccQuery3005.class),
            Map.entry(MessageType.MSG_3006.msgNo(), QyAccQueryReturn3006.class),
            Map.entry(MessageType.MSG_3007.msgNo(), InvoCheckQuery3007.class),
            Map.entry(MessageType.MSG_3008.msgNo(), InvoCheckReturn3008.class),
            Map.entry(MessageType.MSG_3107.msgNo(), PzCheckQuery3107.class),
            Map.entry(MessageType.MSG_3108.msgNo(), PzCheckQueryReturn3108.class),
            Map.entry(MessageType.MSG_3112.msgNo(), HxqyCreditAmt3112.class),
            Map.entry(MessageType.MSG_3115.msgNo(), PlatPay3115.class),
            Map.entry(MessageType.MSG_3116.msgNo(), BankCheckDay3116.class));

    private final SyncMessageProcessorService syncProcessor;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Spring 构造注入。
     *
     * @param syncProcessor  同步流水线，非空
     * @param eventPublisher Spring 事件发布器，非空
     */
    public InboundMessageDispatcher(final SyncMessageProcessorService syncProcessor,
                                    final ApplicationEventPublisher eventPublisher) {
        this.syncProcessor = Objects.requireNonNull(syncProcessor, "syncProcessor");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    /**
     * 分发入站报文：路由 → 同步处理 → body 解析 → 事件发布。
     *
     * <p>事务边界：{@link Propagation#REQUIRED} + 任意异常回滚。listener 在同
     * 事务上下文中执行（Spring {@code @EventListener} 默认同步同事务），
     * listener 抛异常时整事务回滚 — message_process_record 与
     * reconciliation_records 一致提交或一致回滚。</p>
     *
     * @param messageType  4 位数字报文类型，非空（已被 controller {@code @Pattern} 校验）
     * @param transitionNo 8 位业务流水号，非空
     * @param xml          UTF-8 XML payload，非空
     * @return 处理响应：recordId / status / eventPublished
     * @throws FepBusinessException {@link FepErrorCode#MSG_INBOUND_INVALID_TYPE}
     *                               未注册的 messageType
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public InboundMessageResponse dispatch(final String messageType,
                                            final String transitionNo,
                                            final byte[] xml) {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(transitionNo, "transitionNo");
        Objects.requireNonNull(xml, "xml");

        final MessageType type = MessageType.byMsgNo(messageType)
                .orElseThrow(() -> new FepBusinessException(
                        FepErrorCode.MSG_INBOUND_INVALID_TYPE,
                        "messageType=" + LogSanitizer.sanitize(messageType)));

        final MessageProcessRecord record = syncProcessor.processInbound(type, transitionNo, xml);

        boolean eventPublished = false;
        if (record.getStatus() == MessageProcessStatus.COMPLETED) {
            final Object body = tryUnmarshalBody(type, xml);
            final String serialNo = extractSerialNo(body, transitionNo);
            eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                    type, transitionNo, serialNo, body, Instant.now()));
            eventPublished = true;
            LOG.info("inbound dispatched msg={} transitionNo={} recordId={} eventPublished=true",
                    type.msgNo(),
                    LogSanitizer.sanitize(transitionNo),
                    record.getId());
        } else {
            LOG.warn("inbound pipeline non-completed msg={} transitionNo={} recordId={} status={}",
                    type.msgNo(),
                    LogSanitizer.sanitize(transitionNo),
                    record.getId(),
                    record.getStatus());
        }

        return new InboundMessageResponse(record.getId(), record.getStatus().name(), eventPublished);
    }

    /**
     * 按 messageType 查 P3 Phase 2 注册表，使用 per-class 缓存的 JAXBContext
     * 反序列化 CfxMessage 并按 isInstance 过滤取首个匹配的 body POJO。
     *
     * <p><b>Envelope shape contract</b>: 生产 CFX {@code <MSG>} 容器由 XSD 强制
     * BatchHeadXxxx 在前 / Body POJO 在后两子元素并存（参见 PRD §3.2.4）。
     * BatchHead 类型未在 dispatcher JAXBContext 注册，回退为 lax-mode
     * {@code org.w3c.dom.Element}；用 {@code msg.getBody()} 取 position 0
     * 永远拿到 BatchHead DOM Element 而非真 body POJO（P3 Task 5 IT 暴露的
     * 关键 wiring bug — 旧实现 listener body=null silent skip）。</p>
     *
     * <p>修复后逻辑遍历 {@code msg.getBodies()} 并按 {@code bodyClass::isInstance}
     * 过滤，对 (a) 单 body 报文 (b) BatchHead+body 双子元素 (c) 0 命中 (d) 多
     * body 候选 四类 envelope 全部正确。0 命中时返回 null + LOG.warn，由
     * dispatcher caller 处理；listener 端 null-check 兜底。</p>
     *
     * <p>未注册的 messageType 返回 {@code null}（listener 自行降级）；
     * unmarshal 失败 ({@link JAXBException}) 或 JAXBContext 构建失败
     * ({@link IllegalStateException} 由 {@link JaxbContextCache#getForBody}
     * 包装首次 build 失败的 JAXBException) 都抛 {@link FepBusinessException}
     * 让 {@link Transactional} 整事务回滚（v1a P0-Q1 + santa 教训：unmarshal
     * 失败 = 数据不一致风险；R1 closing Q-2 fix：cache build 失败的 ISE 走同一封装路径）。</p>
     *
     * @param type 入站报文类型，非空
     * @param xml  原始 XML，非空
     * @return 解析后的 body POJO 或 {@code null}
     * @throws FepBusinessException unmarshal 或 cache 构建失败时让事务回滚
     */
    private Object tryUnmarshalBody(final MessageType type, final byte[] xml) {
        final Class<?> bodyClass = BODY_TYPE_REGISTRY.get(type.msgNo());
        if (bodyClass == null) {
            return null;
        }
        try {
            final JAXBContext ctx = JaxbContextCache.getForBody(bodyClass);
            final Unmarshaller u = ctx.createUnmarshaller();
            u.setEventHandler(event -> false);
            final CfxMessage msg = (CfxMessage) u.unmarshal(new ByteArrayInputStream(xml));
            // P3 Task 5 finding: production CFX samples carry both BatchHeadXxxx
            // (lax-mode DOM Element) and the registered body class as siblings
            // under <MSG>. Walk the bodies list and pick the first registered-
            // type instance instead of trusting position 0 — otherwise the
            // BatchHead DOM element shadows the real body POJO and the listener
            // receives event.body=null (P3 wiring inert). XSD-required sequence
            // ordering (BatchHead → body) means getBody() is always wrong here.
            for (Object candidate : msg.getBodies()) {
                if (candidate != null && bodyClass.isInstance(candidate)) {
                    return candidate;
                }
            }
            LOG.warn("inbound body type mismatch msg={} expected={} bodies={}",
                    type.msgNo(),
                    bodyClass.getSimpleName(),
                    msg.getBodies().size());
            return null;
        } catch (JAXBException | IllegalStateException e) {
            // R1 closing Q-2 fix: JaxbContextCache.getForBody wraps first-time
            // JAXBContext build failures as IllegalStateException. Without
            // catching it here, the unchecked exception escapes @Transactional
            // and bypasses FepBusinessException envelope used by upstream
            // rollback orchestration.
            throw new FepBusinessException(
                    FepErrorCode.MSG_INBOUND_DECODE_FAILURE,
                    "unmarshal body failed for msg=" + type.msgNo(), e);
        }
    }

    /**
     * 从 body POJO 提取业务 SerialNo；body 为 null、未实现
     * {@link SerialNoBearing} 或 {@code getSerialNo()} 返回 null/empty 时
     * fallback 到 {@code transitionNo}。
     *
     * <p>E-3 重构（2026-05-08）— 改用 {@code instanceof} 模式匹配替代反射 hot path。
     * {@link #BODY_TYPE_REGISTRY} 注册的 body 中实现该接口的子集走类型守卫分支；
     * 不实现该接口的 body（如 {@code DataTransfer2101} 等无业务 SerialNo 字段者）
     * 走 fallback 返回 {@code transitionNo}。注册漂移由 ArchUnit 不变量
     * {@code InboundRegistryArchTest} 保证未来注册新 body 漏 implements 立即
     * 编译期/测试期被抓（具体实现者集合以 grep {@code implements SerialNoBearing} 实测为准）。</p>
     *
     * <p><b>无 LOG 路径</b>（v0.2 santa Round 1 Reviewer 提出）：旧反射版本对
     * {@code NoSuchMethodException} / {@code IllegalAccessException} /
     * {@code InvocationTargetException} 三类异常分别 LOG.debug / LOG.warn 诊断；
     * 新版本 {@code instanceof} 类型守卫 + {@code String?.isEmpty()} 检查不会抛
     * 异常，三条 catch 路径不可达，故 LOG 全部移除。注册漂移由
     * {@code InboundRegistryArchTest} 编译期/测试期捕获，无需运行期日志。</p>
     *
     * <p>v0.3 修订（santa Round 2 Reviewer B'+C' S1/C12/F7）— private 改 package-private
     * 让 {@code InboundMessageDispatcherSerialNoTest} 与 {@code InboundDispatcherSerialNoMicroBenchmark}
     * 不需 Method.setAccessible+invoke 反射调用，benchmark 直接测 instanceof 真实代价
     * （v0.2 反射 wrapper 主导测量使 P95 阈值断言 tautological）。视图：fep-web 内部
     * service 包暴露 default 可见性，跨包仍不可见，封装隔离仍成立。</p>
     *
     * @param body         body POJO，可空；非 {@link SerialNoBearing} 时走 fallback
     * @param transitionNo 备用流水号，非空
     * @return 业务 serialNo（非 null）
     */
    static String extractSerialNo(final Object body, final String transitionNo) {
        if (body instanceof SerialNoBearing s) {
            String value = s.getSerialNo();
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return transitionNo;
    }

    /**
     * 暴露给单测验证 body 反查表内容（避免单测做反射读取私有静态字段）。
     * 返回值是显式 {@link Map#copyOf} 副本，确保调用方无法改写内部静态注册表。
     *
     * @return 17 种 body POJO 注册表的不可变副本
     *         （2101 P4-MSG-D T4 + 2102/2103/2104 P4-MSG-A-inbound + 3001/3002/3003/3004/3005/3006
     *         P4-Plan-C T1 + 3007/3008 P4-MSG-B-inbound + 3107/3108/3115/3116 P3 Phase 2
     *         + 3112 P4-MSG-J）。
     *         底层使用 {@link Map#ofEntries} 不限 entry 数（P4-MSG-D T0 refactor）。
     */
    public static Map<String, Class<?>> bodyTypeRegistry() {
        return Map.copyOf(BODY_TYPE_REGISTRY);
    }
}
