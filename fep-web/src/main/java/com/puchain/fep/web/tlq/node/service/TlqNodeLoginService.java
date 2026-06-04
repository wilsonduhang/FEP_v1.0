package com.puchain.fep.web.tlq.node.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.model.RealHead9005;
import com.puchain.fep.converter.model.RealHead9006;
import com.puchain.fep.converter.model.RealHead9008;
import com.puchain.fep.converter.pipeline.EncodeResult;
import com.puchain.fep.converter.pipeline.MessageEncoder;
import com.puchain.fep.converter.pipeline.MessagePipelineOptions;
import com.puchain.fep.processor.body.common.LoginRequest9006;
import com.puchain.fep.processor.body.common.LogoutRequest9008;
import com.puchain.fep.transport.api.NodeLifecycleManager;
import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.puchain.fep.web.outbound.consumer.BodyMsgIdGenerator;
import com.puchain.fep.web.tlq.node.domain.TlqNode;
import com.puchain.fep.web.tlq.node.repository.TlqNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 节点登录/登出业务编排（P1c T7 v1c / PRD v1.3 §3.7 + §5.7.4）。
 *
 * <p>编排流程：拼 {@link CfxMessage}（{@link CommonHead} + {@link RealHead9006} +
 * {@link LoginRequest9006} 三段）→ fep-converter {@link MessageEncoder#encode}
 * → {@link TlqProducer#send} → {@link NodeLifecycleManager#login}.</p>
 *
 * <p><strong>关键事实</strong>:</p>
 * <ul>
 *   <li>{@code LoginRequest9006} 在 fep-processor.body.common（非 fep-converter）</li>
 *   <li>{@code LoginRequest9006} 仅 2 字段：{@code password / newPassword}</li>
 *   <li>{@code LogoutRequest9008} 仅 1 字段：{@code password}</li>
 *   <li>{@link CommonHead#setDesNode}（非 setDestNode — XML 元素名 DesNode）</li>
 *   <li>{@link CommonHead} 无 setWorkTime（仅 setWorkDate yyyyMMdd 8 位）</li>
 *   <li>{@link CfxMessage#of(CommonHead, Object...)} varargs — 第 2-N 参数都作为
 *       MSG 子元素（P1b-DEFECT-001 修复支持多元素）</li>
 *   <li>{@link TlqNode} 实体仅 12 字段，无 password/userAccount/sessionId 字段</li>
 *   <li>9006/9008 password 来自 {@code fep.transport.tongtech.password}
 *       配置（broker 接入凭证 — TLQ 节点登录是机器→机器认证；通过
 *       {@code @Value} 直接读取，避免依赖 mock provider 下不存在的
 *       {@code TongtechTlqProperties} bean — 见 closing addendum）</li>
 *   <li><b>R-1 (2026-05-06)</b>: 9006/9008 CommonHead.MsgId 改用 {@link BodyMsgIdGenerator}
 *       生成 20 字符全数字（PRD v1.3 §3.1.3 强制）；中间件 corrId（{@link TlqMessageAttributes}）
 *       继续用 {@link IdGenerator#uuid20()}（base36，不受 PRD 约束）。详见
 *       {@code docs/decisions/2026-05-06-bodymsgid-vs-uuid20-rationale.md}</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class TlqNodeLoginService {

    private static final Logger LOG = LoggerFactory.getLogger(TlqNodeLoginService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** HNDEMP 中心节点代码（CLAUDE.md 已知约束 — 14 字符固定值）。R-2 (2026-05-07): 转引用 {@link FepConstants#HNDEMP_NODE_CODE}。 */
    private static final String HNDEMP_DEST_NODE = FepConstants.HNDEMP_NODE_CODE;

    /** 会话首报文 corrMsgId 兜底约定（PRD §3.2.2 corrMsgId 字段说明）。 */
    private static final String CORR_MSG_ID_NEW_SESSION = "00000000000000000000";

    /** TransitionNo 长度（见 RealHead9006/9008 8 位数字校验）。 */
    private static final int TRANSITION_NO_LENGTH = 8;

    /** Decimal radix for字母 → 个位数字映射（保证 \d{8} 校验通过）。 */
    private static final int DECIMAL_RADIX = 10;

    private final NodeLifecycleManager lifecycle;
    private final TlqProducer producer;
    private final MessageEncoder encoder;
    private final TlqNodeRepository nodeRepository;

    /** 14 字符 HNDEMP 发送方机构代码（CommonHead.SrcNode 必须 14 位）。 */
    private final String srcNode;

    /** Broker 接入凭证（password 字段；空字符串在 mock 路径下也合法 — 报文 marshal 不抛）。 */
    private final String brokerPassword;

    /**
     * R-1 (2026-05-06) 注入的 HNDEMP CommonHead.MsgId 全数字生成器（PRD §3.1.3 合规）。
     * 替换 9006/9008 装配从 {@link IdGenerator#uuid20()} (base36 含小写字母 — 违反 PRD §3.1.3)。
     */
    private final BodyMsgIdGenerator bodyMsgIdGenerator;

    /**
     * 构造方法。
     *
     * @param lifecycle           节点生命周期管理器（mock / tongtech 各 1 个 bean）
     * @param producer            TLQ 消息生产者（mock / tongtech）
     * @param encoder             fep-converter 出站编码流水线
     * @param nodeRepository      TLQ 节点 Repository（用于校验节点存在）
     * @param srcNode             14 字符发送方机构代码（{@code fep.transport.institution-code}
     *                            — 默认 {@value com.puchain.fep.common.util.FepConstants#HNDEMP_NODE_CODE} 确保 14 字符长度合法）
     * @param brokerPassword      broker 接入凭证（{@code fep.transport.tongtech.password}
     *                            — 默认空串，仅 tongtech provider 部署时必填）
     * @param bodyMsgIdGenerator  HNDEMP CommonHead.MsgId 全数字生成器（R-1, 2026-05-06）
     */
    public TlqNodeLoginService(
            final NodeLifecycleManager lifecycle,
            final TlqProducer producer,
            final MessageEncoder encoder,
            final TlqNodeRepository nodeRepository,
            @Value("${fep.transport.institution-code:" + HNDEMP_DEST_NODE + "}") final String srcNode,
            @Value("${fep.transport.tongtech.password:}") final String brokerPassword,
            final BodyMsgIdGenerator bodyMsgIdGenerator) {
        this.lifecycle = lifecycle;
        this.producer = producer;
        this.encoder = encoder;
        this.nodeRepository = nodeRepository;
        this.srcNode = srcNode;
        this.brokerPassword = brokerPassword;
        this.bodyMsgIdGenerator = bodyMsgIdGenerator;
    }

    /**
     * 节点登录：拼 9006 → encode → send → lifecycle.login。
     *
     * @param nodeId 目标节点 ID
     * @return {@code true} 当 9006 发送成功且状态成功转 ONLINE；
     *         {@code false} 当发送失败或状态机拒绝转换
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    public boolean login(final String nodeId) {
        final TlqNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5015,
                        "TLQ 节点不存在: " + nodeId));

        final CfxMessage cfx = build9006Message(node);
        final EncodeResult encoded = encoder.encode(cfx, defaultPipelineOpts());
        final String payload = encoded.getPayload();
        final TlqMessage msg = new TlqMessage(
                payload,
                // TLQ 中间件 corrId, 非 CommonHead.MsgId, 不在 PRD §3.1.3 范围（保留 uuid20，R-1 2026-05-06）
                TlqMessageAttributes.forRealtime(IdGenerator.uuid20()),
                TlqChannel.REALTIME_SEND);

        final SendResult result = producer.send(msg);
        if (!result.success()) {
            LOG.warn("9006 send failed nodeId={} error={}",
                    LogSanitizer.sanitize(nodeId),
                    LogSanitizer.sanitize(result.error()));
            return false;
        }
        LOG.info("9006 sent for nodeId={} msgId={}",
                LogSanitizer.sanitize(nodeId),
                LogSanitizer.sanitize(result.msgId()));
        return lifecycle.login();
    }

    /**
     * 节点登出：拼 9008 → encode → send → lifecycle.logout。
     *
     * @param nodeId 目标节点 ID
     * @return {@code true} 当 9008 发送成功且状态成功转 OFFLINE；
     *         {@code false} 当发送失败或状态机拒绝转换
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    public boolean logout(final String nodeId) {
        final TlqNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5015,
                        "TLQ 节点不存在: " + nodeId));

        final CfxMessage cfx = build9008Message(node);
        final EncodeResult encoded = encoder.encode(cfx, defaultPipelineOpts());
        final String payload = encoded.getPayload();
        final TlqMessage msg = new TlqMessage(
                payload,
                // TLQ 中间件 corrId, 非 CommonHead.MsgId, 不在 PRD §3.1.3 范围（保留 uuid20，R-1 2026-05-06）
                TlqMessageAttributes.forRealtime(IdGenerator.uuid20()),
                TlqChannel.REALTIME_SEND);

        final SendResult result = producer.send(msg);
        if (!result.success()) {
            LOG.warn("9008 send failed nodeId={} error={}",
                    LogSanitizer.sanitize(nodeId),
                    LogSanitizer.sanitize(result.error()));
            return false;
        }
        LOG.info("9008 sent for nodeId={} msgId={}",
                LogSanitizer.sanitize(nodeId),
                LogSanitizer.sanitize(result.msgId()));
        return lifecycle.logout();
    }

    /**
     * 节点心跳：拼 head-only 9005 → encode(sign=false) → send，fire-and-forget。
     *
     * <p>区别于 {@link #login}：心跳是 keepalive，<strong>不调 lifecycle 状态机</strong>
     * （不改 ONLINE/OFFLINE）。9005 head-only（无 body，仅 {@link RealHead9005}）。</p>
     *
     * @param nodeId 目标节点 ID，非空
     * @return {@code true} 当 9005 发送成功；{@code false} 当发送失败
     * @throws FepBusinessException 节点不存在（BIZ_5015）
     */
    public boolean heartbeat(final String nodeId) {
        final TlqNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5015,
                        "TLQ 节点不存在: " + nodeId));

        final CfxMessage cfx = build9005Message(node);
        final EncodeResult encoded = encoder.encode(cfx, defaultPipelineOpts());
        final String payload = encoded.getPayload();
        final TlqMessage msg = new TlqMessage(
                payload,
                // TLQ 中间件 corrId, 非 CommonHead.MsgId, 不在 PRD §3.1.3 范围（保留 uuid20）
                TlqMessageAttributes.forRealtime(IdGenerator.uuid20()),
                TlqChannel.REALTIME_SEND);

        final SendResult result = producer.send(msg);
        if (!result.success()) {
            LOG.warn("9005 heartbeat send failed nodeId={} error={}",
                    LogSanitizer.sanitize(nodeId),
                    LogSanitizer.sanitize(result.error()));
            return false;
        }
        LOG.info("9005 heartbeat sent for nodeId={} msgId={}",
                LogSanitizer.sanitize(nodeId),
                LogSanitizer.sanitize(result.msgId()));
        return true;
    }

    /**
     * 9005 节点心跳请求报文装配（head-only，结构同 9006 但无 body）。
     *
     * <p>仅装配两段：{@link CommonHead}（MsgNo=9005）+ {@link RealHead9005}（3 字段）；
     * {@link CfxMessage#of(CommonHead, Object...)} 单 body 元素即 head-only 心跳。</p>
     *
     * @param node 目标节点（仅校验存在，业务字段不依赖节点属性 — 走 broker 凭证）
     * @return 装配好的 head-only CfxMessage
     */
    private CfxMessage build9005Message(final TlqNode node) {
        final String msgId = bodyMsgIdGenerator.generate();   // PRD §3.1.3 全数字格式
        final String workDate = LocalDateTime.now().format(DATE_FMT);

        final CommonHead commonHead = new CommonHead();
        commonHead.setVersion("1.0");
        commonHead.setSrcNode(srcNode);
        commonHead.setDesNode(HNDEMP_DEST_NODE);
        commonHead.setApp("HNDEMP");
        commonHead.setMsgNo("9005");
        commonHead.setMsgId(msgId);
        commonHead.setCorrMsgId(CORR_MSG_ID_NEW_SESSION);
        commonHead.setWorkDate(workDate);

        final RealHead9005 realHead = new RealHead9005();
        realHead.setSendOrgCode(srcNode);
        realHead.setEntrustDate(workDate);
        realHead.setTransitionNo(deriveTransitionNo(msgId));

        return CfxMessage.of(commonHead, realHead);   // head-only，无 body
    }

    /**
     * 9006 节点登录请求报文装配。
     *
     * <p>装配三段：</p>
     * <ol>
     *   <li>{@link CommonHead} — 路由层（12 字段，含 14 位 SrcNode/DesNode 校验）</li>
     *   <li>{@link RealHead9006} — 业务头（3 字段，与 RequestHead 同结构）</li>
     *   <li>{@link LoginRequest9006} — Body（仅 password）</li>
     * </ol>
     *
     * <p>R-1 (2026-05-06): MsgId 改用 {@link BodyMsgIdGenerator#generate()} 输出 20 字符全数字
     * （PRD v1.3 §3.1.3 强制：日期时间 14 位 + 顺序号 6 位）。原 {@link IdGenerator#uuid20()}
     * 输出 base36 含小写字母，违反 PRD §3.1.3。</p>
     *
     * @param node 目标节点（当前实现仅校验存在，业务字段不依赖节点属性 — 走 broker 凭证）
     * @return 装配好的 CfxMessage
     */
    private CfxMessage build9006Message(final TlqNode node) {
        final String msgId = bodyMsgIdGenerator.generate();   // PRD §3.1.3 全数字格式 (R-1 swap, 2026-05-06)
        final String workDate = LocalDateTime.now().format(DATE_FMT);

        final CommonHead commonHead = new CommonHead();
        commonHead.setVersion("1.0");
        commonHead.setSrcNode(srcNode);
        commonHead.setDesNode(HNDEMP_DEST_NODE);
        commonHead.setApp("HNDEMP");
        commonHead.setMsgNo("9006");
        commonHead.setMsgId(msgId);
        commonHead.setCorrMsgId(CORR_MSG_ID_NEW_SESSION);
        commonHead.setWorkDate(workDate);

        final RealHead9006 realHead = new RealHead9006();
        realHead.setSendOrgCode(srcNode);
        realHead.setEntrustDate(workDate);
        realHead.setTransitionNo(deriveTransitionNo(msgId));

        final LoginRequest9006 body = new LoginRequest9006();
        body.setPassword(brokerPassword);
        // newPassword 可选；P1c 默认不设（密码轮换场景由 P3 follow-up 处理）

        return CfxMessage.of(commonHead, realHead, body);
    }

    /**
     * 9008 节点登出请求报文装配（结构同 9006，Body 改 LogoutRequest9008）。
     *
     * <p>R-1 (2026-05-06): MsgId 同 9006 改用 {@link BodyMsgIdGenerator#generate()}（PRD §3.1.3）。</p>
     *
     * @param node 目标节点
     * @return 装配好的 CfxMessage
     */
    private CfxMessage build9008Message(final TlqNode node) {
        final String msgId = bodyMsgIdGenerator.generate();   // PRD §3.1.3 全数字格式 (R-1 swap, 2026-05-06)
        final String workDate = LocalDateTime.now().format(DATE_FMT);

        final CommonHead commonHead = new CommonHead();
        commonHead.setVersion("1.0");
        commonHead.setSrcNode(srcNode);
        commonHead.setDesNode(HNDEMP_DEST_NODE);
        commonHead.setApp("HNDEMP");
        commonHead.setMsgNo("9008");
        commonHead.setMsgId(msgId);
        commonHead.setCorrMsgId(CORR_MSG_ID_NEW_SESSION);
        commonHead.setWorkDate(workDate);

        final RealHead9008 realHead = new RealHead9008();
        realHead.setSendOrgCode(srcNode);
        realHead.setEntrustDate(workDate);
        realHead.setTransitionNo(deriveTransitionNo(msgId));

        final LogoutRequest9008 body = new LogoutRequest9008();
        body.setPassword(brokerPassword);

        return CfxMessage.of(commonHead, realHead, body);
    }

    /**
     * 节点登录/登出报文流水线选项：sign=false（节点登录前还无业务密钥）+
     * zip=false + encrypt=false（9006/9008 报文体小且 broker 内部链路）。
     *
     * @return pipeline 选项
     */
    private MessagePipelineOptions defaultPipelineOpts() {
        final MessagePipelineOptions opts = new MessagePipelineOptions();
        opts.setSign(false);
        return opts;
    }

    /**
     * 派生 8 位数字 TransitionNo：取 msgId 末 8 字符并把字母替换为数字（保证 RealHead*
     * 的 {@code \d{8}} 校验通过）。
     *
     * <p>R-1 (2026-05-06) 后 msgId 由 {@link BodyMsgIdGenerator#generate()} 生成
     * 20 字符全数字，末 8 位已是数字，字母替换分支变为 dead code（保留以兼容
     * 未来 generator 切换；不阻塞当前路径，可留 V3 ticket 清理）。</p>
     *
     * @param msgId 20 字符 MsgId（R-1 后全数字；历史 base36 兼容路径保留）
     * @return 8 字符纯数字流水号
     */
    private String deriveTransitionNo(final String msgId) {
        final int len = msgId.length();
        final String tail = msgId.substring(Math.max(0, len - TRANSITION_NO_LENGTH));
        final StringBuilder sb = new StringBuilder(TRANSITION_NO_LENGTH);
        for (int i = 0; i < tail.length(); i++) {
            final char c = tail.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else {
                // base36 字母 a-z (10-35) → 取个位数（确保 ASCII 数字）
                sb.append((char) ('0' + ((c - 'a') % DECIMAL_RADIX)));
            }
        }
        return sb.toString();
    }
}
