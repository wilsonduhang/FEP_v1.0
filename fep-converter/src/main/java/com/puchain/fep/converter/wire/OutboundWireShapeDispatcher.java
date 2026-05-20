package com.puchain.fep.converter.wire;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.RequestResponseHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 出站报文 wire-shape 路由 — 单一真相源，决定 {@value #REGISTERED_MSG_NO_COUNT} 上行报文 msgNo 对应的
 * head 元素名 / head 类型 / 是否要求 ResultCode（PRD v1.3 §3.2 + §4.6）。
 *
 * <p>实测自 {@value #REGISTERED_MSG_NO_COUNT} 份 XSD（{@code fep-processor/src/main/resources/xsd/{1001,1004,1101,1102,1103,1104,
 * 2001,2004,2102,2103,2104,3000,3001,3002,3003,3004,3005,3006,3007,3008,3009,3020,3101,3102,3103,3105,
 * 3107,3108,3109,3112,3116}.xsd}）：</p>
 * <ul>
 *   <li>1001 → {@code RealHead1001} + {@link RequestBusinessHead}（企业信息实时查询请求，P4-MSG-E T2）</li>
 *   <li>1004 → {@code RealHead1004} + {@link RequestBusinessHead}（授权书实时发送请求，P4-MSG-E T2）</li>
 *   <li>1101 → {@code BatchHead1101} + {@link RequestBusinessHead}（外联机构数据报送，P4-MSG-D T3）</li>
 *   <li>1102 → {@code BatchHead1102} + {@link RequestBusinessHead}（数据报送核对请求，P4-MSG-A T1）</li>
 *   <li>1103 → {@code BatchHead1103} + {@link RequestBusinessHead}（企业信息批量查询请求，P4-MSG-A T1）</li>
 *   <li>1104 → {@code BatchHead1104} + {@link RequestBusinessHead}（授权书批量上传请求，P4-MSG-A T1）</li>
 *   <li>2001 → {@code RealHead2001} + {@link ResponseBusinessHead}（实时查询回执，含 ResultCode，P4-MSG-E T2 新类目）</li>
 *   <li>2004 → {@code RealHead2004} + {@link ResponseBusinessHead}（授权书回执，含 ResultCode，P4-MSG-E T2 新类目）</li>
 *   <li>2102 → {@code BatchHead2102} + {@link ResponseBusinessHead}（数据报送核对回执，含 ResultCode，P4-MSG-A T1）</li>
 *   <li>2103 → {@code BatchHead2103} + {@link ResponseBusinessHead}（企业信息批量查询回执，含 ResultCode，P4-MSG-A T1）</li>
 *   <li>2104 → {@code BatchHead2104} + {@link ResponseBusinessHead}（授权书批量上传回执，含 ResultCode，P4-MSG-A T1）</li>
 *   <li>3000 → {@code RealHead3000} + {@link RequestBusinessHead}（电子凭证信息报送，模式 3 异步，P4-MSG-B T4）</li>
 *   <li>3001 → {@code RealHead3001} + {@link RequestBusinessHead}（业务进展实时查询请求，P4-MSG-F T2）</li>
 *   <li>3002 → {@code RealHead3002} + {@link ResponseBusinessHead}（业务进展查询回执，含 ResultCode，P4-MSG-F T2）</li>
 *   <li>3003 → {@code RealHead3003} + {@link RequestBusinessHead}（凭证融资状态查询请求，P4-MSG-F T2）</li>
 *   <li>3004 → {@code RealHead3004} + {@link ResponseBusinessHead}（凭证融资状态查询回执，含 ResultCode，P4-MSG-F T2）</li>
 *   <li>3005 → {@code RealHead3005} + {@link RequestBusinessHead}（对公账户状态查询请求，P4-MSG-F T2）</li>
 *   <li>3006 → {@code RealHead3006} + {@link ResponseBusinessHead}（对公客户状态查询回执，含 ResultCode，P4-MSG-F T2）</li>
 *   <li>3007 → {@code RealHead3007} + {@link RequestBusinessHead}（受理单位发起核验请求，模式 1 同步，P4-MSG-B T1）</li>
 *   <li>3008 → {@code RealHead3008} + {@link ResponseBusinessHead}（发票核验回执，含 ResultCode，P4-MSG-G T3）</li>
 *   <li>3009 → {@code RealHead3009} + {@link RequestBusinessHead}（电子凭证融资结果登记，P5 T4）</li>
 *   <li>3020 → {@code RealHead3020} + {@link RequestResponseHead}（供应链实时业务通用转发，孤儿成员第 5 类目，P4-MSG-G T3）</li>
 *   <li>3101 → {@code BatchHead3101} + {@link ResponseBusinessHead}（含 5 位 ResultCode，仅一个）</li>
 *   <li>3102/3105/3107/3109/3112/3116 → {@code BatchHead{msgNo}} + {@link RequestBusinessHead}</li>
 *   <li>3103 → {@code BatchHead3103} + {@link ResponseBusinessHead}（企业建档信息回执，含 ResultCode，P4-MSG-G T3）</li>
 *   <li>3108 → {@code BatchHead3108} + {@link ResponseBusinessHead}（平台凭证核对回执，含 ResultCode，P4-MSG-G T3）</li>
 *   <li>3115 → {@code BatchHead3115} + {@link RequestResponseHead}（资金清算信息指令及回执，第 6 类目，P4-MSG-H）</li>
 *   <li>3120 → {@code BatchHead3120} + {@link RequestBusinessHead}（供应链非实时业务通用转发，第 2 类目扩展，P4-MSG-H）</li>
 *   <li>9000 → {@code RealHead9000} + {@link RequestBusinessHead}（实时业务通用转发，P4-MSG-I）</li>
 *   <li>9100 → {@code BatchHead9100} + {@link RequestBusinessHead}（非实时业务通用转发，模式3，P4-MSG-I）</li>
 *   <li>3113 → {@code BatchHead3113} + {@link ResponseBusinessHead}（核心企业授信额度回执，含 ResultCode，P4-MSG-I）</li>
 *   <li>9120 → {@code BatchHead9120} + {@link ResponseBusinessHead}（通用应答，2101 模式6 ack，含 ResultCode，P4-MSG-I）</li>
 * </ul>
 *
 * <p>6 类 wire-shape (P4-MSG-E T2 起新增 RealHead + ResponseBusinessHead + true 类目；
 * P4-MSG-F T2 起 RealHead+Request 扩展 3001/3003/3005，RealHead+Response 扩展 3002/3004/3006；
 * P4-MSG-G T3 起新增第 5 类 RealHead + RequestResponseHead + false 孤儿类目，并扩展类目 3/4；
 * P4-MSG-H 起新增第 6 类 BatchHead + RequestResponseHead + false，并扩展类目 3)：</p>
 * <ul>
 *   <li>RealHead + RequestBusinessHead + false: 1001/1004/3000/3001/3003/3005/3007/3009/9000（P4-MSG-I 扩展 9000）</li>
 *   <li>RealHead + ResponseBusinessHead + true: 2001/2004/3002/3004/3006/3008（P4-MSG-E T2 新类目，P4-MSG-F/G 扩展）</li>
 *   <li>BatchHead + RequestBusinessHead + false: 1101/1102/1103/1104/3102/3105/3107/3109/3112/3116/3120/9100
 *       （P4-MSG-H 扩展 3120，P4-MSG-I 扩展 9100）</li>
 *   <li>BatchHead + ResponseBusinessHead + true: 3101/3103/3108/2102/2103/2104/3113/9120
 *       （3101 历史归类于此类目，参见 PRD v1.3 §4.6；3103/3108 P4-MSG-G T3 扩展；3113/9120 P4-MSG-I 扩展）</li>
 *   <li>RealHead + RequestResponseHead + false: 3020（孤儿成员）</li>
 *   <li>BatchHead + RequestResponseHead + false: 3115
 *       （P4-MSG-H 新第 6 类目；与第 5 类目 3020 同 RequestResponseHead 类型，head 前缀 BatchHead 而非 RealHead）</li>
 * </ul>
 *
 * <p>P5 T3：消除 inbound {@code BatchMessageProcessorService.wrapBodyInCfx} 与 outbound
 * {@code OutboundCfxEnvelopeBuilder} 的两条 head 元素拼装路径分歧。两者统一通过
 * {@link #describeFor(String)} 拿描述符。</p>
 *
 * <p>非法 msgNo（{@code null} / 非 4 位数字 / 不在 {@value #REGISTERED_MSG_NO_COUNT} 集合）抛
 * {@link FepBusinessException} + {@link FepErrorCode#OUTBOUND_5108_MSGNO_INVALID}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundWireShapeDispatcher {

    /** 4 位数字 msgNo 校验正则。 */
    private static final String MSG_NO_PATTERN = "\\d{4}";

    /** 预编译的 {@link #MSG_NO_PATTERN}，避免 describeFor / isRegisteredOutboundMsgNo 热路径每次重编译。 */
    private static final Pattern MSG_NO_COMPILED = Pattern.compile(MSG_NO_PATTERN);

    /** RealHead + {@link RequestBusinessHead} + false 类目 msgNo 集合（P4-MSG-I 扩展 9000）。 */
    public static final Set<String> REAL_HEAD_REQUEST_MSG_NOS = Set.of(
            "1001", "1004", "3000", "3001", "3003", "3005", "3007", "3009", "9000");

    /** BatchHead + {@link RequestBusinessHead} + false 类目 msgNo 集合（P4-MSG-H 扩展 3120；P4-MSG-I 扩展 9100）。 */
    public static final Set<String> BATCH_HEAD_REQUEST_MSG_NOS = Set.of(
            "1101", "1102", "1103", "1104",
            "3102", "3105", "3107", "3109", "3112", "3116", "3120", "9100");

    /** RealHead + {@link ResponseBusinessHead} + true 类目 msgNo 集合（P4-MSG-G T3 扩展 3008）。 */
    public static final Set<String> REAL_HEAD_RESPONSE_MSG_NOS = Set.of(
            "2001", "2004", "3002", "3004", "3006", "3008");

    /** BatchHead + {@link ResponseBusinessHead} + true 类目 msgNo 集合（P4-MSG-G T3 扩展 3103/3108；P4-MSG-I 扩展 3113/9120）。 */
    public static final Set<String> BATCH_HEAD_RESPONSE_MSG_NOS = Set.of(
            "2102", "2103", "2104", "3101", "3103", "3108", "3113", "9120");

    /**
     * RealHead + {@link RequestResponseHead} + false 第 5 类目 msgNo 集合
     * （P4-MSG-G T3 新增孤儿类目；3115 实测 BatchHead 前缀已移入第 6 类目 P4-MSG-H，
     * 3120 实测 RequestHead 类型已移入第 2 类目 P4-MSG-H — 衔接 prompt "同 family" 假设经 grep 推翻）。
     */
    public static final Set<String> REAL_HEAD_REQUEST_RESPONSE_MSG_NOS = Set.of(
            "3020");

    /**
     * BatchHead + {@link RequestResponseHead} + false 第 6 类目 msgNo 集合
     * （P4-MSG-H 新增；3115 资金清算信息指令及回执，BatchHead3115 type=RequestResponseHead，
     * 与第 5 类目 3020 同 RequestResponseHead 类型但 head 元素前缀为 BatchHead 而非 RealHead）。
     */
    public static final Set<String> BATCH_HEAD_REQUEST_RESPONSE_MSG_NOS = Set.of(
            "3115");

    /** 已登记上行报文总数（Javadoc {@value} 自更新引用）. */
    public static final int REGISTERED_MSG_NO_COUNT = 37;

    /**
     * 路由 msgNo → {@link WireShapeDescriptor}。
     *
     * @param msgNo 4 位数字报文号（{@code "3009"} / {@code "3101"} / ...）
     * @return wire-shape 描述符
     * @throws FepBusinessException msgNo 为 {@code null} / 非 4 位数字 / 不在 {@value #REGISTERED_MSG_NO_COUNT} 上行报文集合，
     *                              错误码 {@link FepErrorCode#OUTBOUND_5108_MSGNO_INVALID}
     */
    public WireShapeDescriptor describeFor(final String msgNo) {
        if (msgNo == null || !MSG_NO_COMPILED.matcher(msgNo).matches()) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5108_MSGNO_INVALID,
                    "msgNo 必须为 4 位数字: " + msgNo);
        }
        if (REAL_HEAD_REQUEST_MSG_NOS.contains(msgNo)) {
            return new WireShapeDescriptor(
                    "RealHead" + msgNo, RequestBusinessHead.class, false);
        }
        if (BATCH_HEAD_REQUEST_MSG_NOS.contains(msgNo)) {
            return new WireShapeDescriptor(
                    "BatchHead" + msgNo, RequestBusinessHead.class, false);
        }
        if (REAL_HEAD_RESPONSE_MSG_NOS.contains(msgNo)) {
            return new WireShapeDescriptor(
                    "RealHead" + msgNo, ResponseBusinessHead.class, true);
        }
        if (BATCH_HEAD_RESPONSE_MSG_NOS.contains(msgNo)) {
            return new WireShapeDescriptor(
                    "BatchHead" + msgNo, ResponseBusinessHead.class, true);
        }
        if (REAL_HEAD_REQUEST_RESPONSE_MSG_NOS.contains(msgNo)) {
            return new WireShapeDescriptor(
                    "RealHead" + msgNo, RequestResponseHead.class, false);
        }
        if (BATCH_HEAD_REQUEST_RESPONSE_MSG_NOS.contains(msgNo)) {
            return new WireShapeDescriptor(
                    "BatchHead" + msgNo, RequestResponseHead.class, false);
        }
        throw new FepBusinessException(
                FepErrorCode.OUTBOUND_5108_MSGNO_INVALID,
                "msgNo 不在 " + REGISTERED_MSG_NO_COUNT + " 上行报文集合: " + msgNo);
    }

    /**
     * 判断 msgNo 是否在已登记的 {@value #REGISTERED_MSG_NO_COUNT} 上行报文集合内。
     *
     * <p>用于 {@code BatchMessageProcessorService.wrapBodyInCfx} 等 inbound + outbound
     * 共用方法识别"是否为已登记 outbound msgNo"，未登记的（例如 9005 心跳类
     * 通用报文，9005.xsd MSG 下无 body 元素）走 legacy 路径，避免对 inbound 链路产生回归。</p>
     *
     * @param msgNo 4 位数字报文号；{@code null} 或非法格式返回 {@code false}
     * @return {@code true} 当且仅当 msgNo 是 {@value #REGISTERED_MSG_NO_COUNT} 上行报文之一
     */
    public boolean isRegisteredOutboundMsgNo(final String msgNo) {
        if (msgNo == null || !MSG_NO_COMPILED.matcher(msgNo).matches()) {
            return false;
        }
        return REAL_HEAD_REQUEST_MSG_NOS.contains(msgNo)
                || BATCH_HEAD_REQUEST_MSG_NOS.contains(msgNo)
                || REAL_HEAD_RESPONSE_MSG_NOS.contains(msgNo)
                || BATCH_HEAD_RESPONSE_MSG_NOS.contains(msgNo)
                || REAL_HEAD_REQUEST_RESPONSE_MSG_NOS.contains(msgNo)
                || BATCH_HEAD_REQUEST_RESPONSE_MSG_NOS.contains(msgNo);
    }
}
