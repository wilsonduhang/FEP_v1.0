package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.util.FepConstants;
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
 * <p>输入三源：</p>
 * <ul>
 *   <li>{@link OutboundMessageQueueEntity}（消费侧 entity）— 提供 {@code messageType}（→ MsgNo）</li>
 *   <li>{@link OutboundHeadFields}（P4 反序列化自 {@code message_head_xml}）— 提供
 *       {@code sendOrgCode} / {@code entrustDate} / {@code transitionNo}</li>
 *   <li>{@code msgId}（由 {@link OutboundCfxEnvelopeBuilder} 在 build 入口生成，透传至此）—
 *       20 位全数字，满足 XSD {@code MsgId} 类型（Number + length 20）</li>
 * </ul>
 *
 * <p>固定字段：</p>
 * <ul>
 *   <li>{@code DesNode} 恒 {@link FepConstants#HNDEMP_NODE_CODE}（HNDEMP 中心节点代码，CLAUDE.md 已知约束）</li>
 *   <li>{@code Version} 恒 "1.0"（CommonHead 默认）</li>
 *   <li>{@code App} 恒 {@link #APP_CODE}（"HNDEMP"）— PRD v1.3 §3.2.2 固定值，XSD App 类型 minLength=4 / maxLength=20</li>
 *   <li>{@code MsgId} 由 build 透传（{@link OutboundCfxEnvelopeBuilder} 生成的 20 位数字），
 *       满足 {@code Base.xsd} HEAD {@code MsgId} 类型（Number + length 20）</li>
 *   <li>{@code CorrMsgId} 新请求恒 {@link #CORR_MSG_ID_NONE}（20 位全零，满足 XSD MsgId 类型必填约束；
 *       响应类报文真关联 id 由 Layer 2（OutboundHeadFields 扩展）回填）</li>
 *   <li>{@code WorkDate} {@code Asia/Shanghai} 当日 yyyyMMdd</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CommonHeadComposer {

    /** HNDEMP 中心节点代码（PRD v1.3 §3.2.2，CLAUDE.md 已知约束）。R-2 (2026-05-07): 转引用 {@link FepConstants#HNDEMP_NODE_CODE}。 */
    private static final String HNDEMP_NODE = FepConstants.HNDEMP_NODE_CODE;

    /**
     * 应用代码 — PRD v1.3 §3.2.2 固定 "HNDEMP"（App 类型 XSD minLength=4，"FEP" 3 字符不合规）。
     */
    static final String APP_CODE = "HNDEMP";

    /** 新请求无关联报文时 CorrMsgId 占位 — 20 位全零满足 MsgId 类型（Number+length 20）。
     *  响应类报文真关联 id 由 Layer 2（OutboundHeadFields 扩展）回填。 */
    static final String CORR_MSG_ID_NONE = "00000000000000000000";

    /** Asia/Shanghai 时区 — 报文 WorkDate 业务时区。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    /** WorkDate 8 位 yyyyMMdd 格式化器。 */
    private static final DateTimeFormatter WORK_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 组装 {@link CommonHead}。
     *
     * @param entity     消费侧 entity（提供 {@code messageType}），非 {@code null}
     * @param headFields 反序列化自 {@code entity.getMessageHeadXml()} 的 {@link OutboundHeadFields}，非 {@code null}
     * @param msgId      由 {@link OutboundCfxEnvelopeBuilder} 生成的 20 位全数字报文标识号，非 {@code null}
     * @return 构造好的 CommonHead；{@code MsgId} = 传入的 {@code msgId}，
     *         {@code CorrMsgId} = {@link #CORR_MSG_ID_NONE}（20 位全零）
     */
    public CommonHead compose(final OutboundMessageQueueEntity entity,
                              final OutboundHeadFields headFields,
                              final String msgId) {
        final CommonHead head = new CommonHead();
        head.setVersion("1.0");
        head.setSrcNode(headFields.sendOrgCode());
        head.setDesNode(HNDEMP_NODE);
        head.setApp(APP_CODE);
        head.setMsgNo(entity.getMessageType());
        head.setMsgId(msgId);
        // 新请求 corrMsgId 恒 20 位全零（满足 XSD MsgId 类型 + Base.xsd HEAD CorrMsgId minOccurs=1 必填约束）
        head.setCorrMsgId(CORR_MSG_ID_NONE);
        head.setWorkDate(LocalDate.now(BIZ_ZONE).format(WORK_DATE_FMT));
        return head;
    }
}
