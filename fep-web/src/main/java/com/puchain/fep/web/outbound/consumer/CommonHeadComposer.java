package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 出站报文 {@link CommonHead} 组装器（PRD v1.3 §3.2.2 + §3.1.3，P5 T4）。
 *
 * <p>输入两源：</p>
 * <ul>
 *   <li>{@link OutboundMessageQueueEntity}（消费侧 entity）— 提供 {@code messageType}（→ MsgNo）</li>
 *   <li>{@link OutboundHeadFields}（P4 反序列化自 {@code message_head_xml}）— 提供
 *       {@code sendOrgCode} / {@code entrustDate} / {@code transitionNo}</li>
 * </ul>
 *
 * <p>固定字段：</p>
 * <ul>
 *   <li>{@code DesNode} 恒 "A1000143000104"（HNDEMP 中心节点代码，CLAUDE.md 已知约束）</li>
 *   <li>{@code Version} 恒 "1.0"（CommonHead 默认）</li>
 *   <li>{@code App} 恒 "FEP"（PRD §3.2.2 应用代码上行恒 FEP，inbound 侧 default "HNDEMP" 由 CommonHead 模型承担）</li>
 *   <li>{@code MsgId} 占位 "PLACEHOLDER_T6_INJEC"（20 位）— Simplify Q-2 修订：占位
 *       不在 marshal 后被替换。{@link OutboundTlqSender#send(String)} 不修改 signed XML，
 *       而是 {@link BodyMsgIdGenerator} 单独生成 20 位 msgId 持久化到
 *       {@code outbound_message_queue.msg_id} + {@link com.puchain.fep.transport.model.TlqMessageAttributes#forBatch(String)
 *       TlqMessageAttributes.forBatch} 入参。CFX envelope 内 MsgId 仍保留占位（业务侧
 *       通过 DB msg_id 列做幂等关联，非 envelope 内字段）。</li>
 *   <li>{@code CorrMsgId} 上行新请求恒 {@code null}（spec N3：未来响应消息回填扩展 OutboundHeadFields）</li>
 *   <li>{@code WorkDate} {@code Asia/Shanghai} 当日 yyyyMMdd</li>
 * </ul>
 *
 * <p><b>Plan 偏离记录</b>：Plan §Step 6 指定 msgId 占位 "PLACEHOLDER_T6_INJECTS" 22 位，
 * 但 {@link CommonHead#setMsgId(String)} 强制 20 位长度（PRD §3.2.2 MsgIdLength
 * 14 datetime + 6 seq）。本实现改用 20 位合成 "PLACEHOLDER_T6_INJEC" 避免 setter IAE，
 * 占位语义不变。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CommonHeadComposer {

    /** HNDEMP 中心节点代码（PRD v1.3 §3.2.2，CLAUDE.md 已知约束）。 */
    private static final String HNDEMP_NODE = "A1000143000104";

    /** 占位 MsgId — 20 位以满足 {@link CommonHead#setMsgId} 长度约束，T6 注入真实值。 */
    static final String MSG_ID_PLACEHOLDER = "PLACEHOLDER_T6_INJEC";

    /** Asia/Shanghai 时区 — 报文 WorkDate 业务时区。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    /** WorkDate 8 位 yyyyMMdd 格式化器。 */
    private static final DateTimeFormatter WORK_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 组装 {@link CommonHead}。
     *
     * @param entity     消费侧 entity（提供 {@code messageType}），非 {@code null}
     * @param headFields 反序列化自 {@code entity.getMessageHeadXml()} 的 {@link OutboundHeadFields}，非 {@code null}
     * @return 构造好的 CommonHead；msgId 占位 {@link #MSG_ID_PLACEHOLDER}，corrMsgId {@code null}
     */
    public CommonHead compose(final OutboundMessageQueueEntity entity, final OutboundHeadFields headFields) {
        final CommonHead head = new CommonHead();
        head.setVersion("1.0");
        head.setSrcNode(headFields.sendOrgCode());
        head.setDesNode(HNDEMP_NODE);
        head.setApp("FEP");
        head.setMsgNo(entity.getMessageType());
        head.setMsgId(MSG_ID_PLACEHOLDER);
        // 上行新请求 corrMsgId 恒 null（spec N3：响应消息回填由 P4 协调扩展 OutboundHeadFields）
        head.setCorrMsgId(null);
        head.setWorkDate(LocalDate.now(BIZ_ZONE).format(WORK_DATE_FMT));
        return head;
    }
}
