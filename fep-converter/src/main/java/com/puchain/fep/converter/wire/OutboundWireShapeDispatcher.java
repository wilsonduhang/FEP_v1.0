package com.puchain.fep.converter.wire;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import org.springframework.stereotype.Component;

/**
 * 出站报文 wire-shape 路由 — 单一真相源，决定 21 上行报文 msgNo 对应的
 * head 元素名 / head 类型 / 是否要求 ResultCode（PRD v1.3 §3.2 + §4.6）。
 *
 * <p>实测自 21 份 XSD（{@code fep-processor/src/main/resources/xsd/{1001,1004,1101,1102,1103,1104,
 * 2001,2004,2102,2103,2104,3000,3007,3009,3101,3102,3105,3107,3109,3112,3116}.xsd}）：</p>
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
 *   <li>3007 → {@code RealHead3007} + {@link RequestBusinessHead}（受理单位发起核验请求，模式 1 同步，P4-MSG-B T1）</li>
 *   <li>3009 → {@code RealHead3009} + {@link RequestBusinessHead}（实时单笔）</li>
 *   <li>3101 → {@code BatchHead3101} + {@link ResponseBusinessHead}（含 5 位 ResultCode，仅一个）</li>
 *   <li>3102/3105/3107/3109/3112/3116 → {@code BatchHead{msgNo}} + {@link RequestBusinessHead}</li>
 * </ul>
 *
 * <p>4 类 wire-shape (P4-MSG-E T2 起新增 RealHead + ResponseBusinessHead + true 类目，仅 2001/2004)：</p>
 * <ul>
 *   <li>RealHead + RequestBusinessHead + false: 1001/1004/3000/3007/3009</li>
 *   <li>RealHead + ResponseBusinessHead + true: 2001/2004（P4-MSG-E T2 新类目）</li>
 *   <li>BatchHead + RequestBusinessHead + false: 1101/1102/1103/1104/3102/3105/3107/3109/3112/3116</li>
 *   <li>BatchHead + ResponseBusinessHead + true: 2102/2103/2104/3101</li>
 * </ul>
 *
 * <p>P5 T3：消除 inbound {@code BatchMessageProcessorService.wrapBodyInCfx} 与 outbound
 * {@code OutboundCfxEnvelopeBuilder} 的两条 head 元素拼装路径分歧。两者统一通过
 * {@link #describeFor(String)} 拿描述符。</p>
 *
 * <p>非法 msgNo（{@code null} / 非 4 位数字 / 不在 21 集合）抛
 * {@link FepBusinessException} + {@link FepErrorCode#OUTBOUND_5108_MSGNO_INVALID}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundWireShapeDispatcher {

    /** 4 位数字 msgNo 校验正则。 */
    private static final String MSG_NO_PATTERN = "\\d{4}";

    /**
     * 路由 msgNo → {@link WireShapeDescriptor}。
     *
     * @param msgNo 4 位数字报文号（{@code "3009"} / {@code "3101"} / ...）
     * @return wire-shape 描述符
     * @throws FepBusinessException msgNo 为 {@code null} / 非 4 位数字 / 不在 21 上行报文集合，
     *                              错误码 {@link FepErrorCode#OUTBOUND_5108_MSGNO_INVALID}
     */
    public WireShapeDescriptor describeFor(final String msgNo) {
        if (msgNo == null || !msgNo.matches(MSG_NO_PATTERN)) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5108_MSGNO_INVALID,
                    "msgNo 必须为 4 位数字: " + msgNo);
        }
        return switch (msgNo) {
            case "1001", "1004",
                 "3000", "3007", "3009" -> new WireShapeDescriptor(
                    "RealHead" + msgNo, RequestBusinessHead.class, false);
            case "1101", "1102", "1103", "1104",
                 "3102", "3105", "3107", "3109", "3112", "3116" -> new WireShapeDescriptor(
                    "BatchHead" + msgNo, RequestBusinessHead.class, false);
            case "2001", "2004" -> new WireShapeDescriptor(
                    "RealHead" + msgNo, ResponseBusinessHead.class, true);
            case "3101", "2102", "2103", "2104" -> new WireShapeDescriptor(
                    "BatchHead" + msgNo, ResponseBusinessHead.class, true);
            default -> throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5108_MSGNO_INVALID,
                    "msgNo 不在 21 上行报文集合: " + msgNo);
        };
    }

    /**
     * 判断 msgNo 是否在已登记的 21 上行报文集合内。
     *
     * <p>用于 {@code BatchMessageProcessorService.wrapBodyInCfx} 等 inbound + outbound
     * 共用方法识别"是否为已登记 outbound msgNo"，未登记的（例如 inbound-only 的 3003 /
     * 3005 / 9000）走 legacy 路径，避免对 inbound 链路产生回归。</p>
     *
     * @param msgNo 4 位数字报文号；{@code null} 或非法格式返回 {@code false}
     * @return {@code true} 当且仅当 msgNo 是 21 上行报文之一
     */
    public boolean isRegisteredOutboundMsgNo(final String msgNo) {
        if (msgNo == null || !msgNo.matches(MSG_NO_PATTERN)) {
            return false;
        }
        return switch (msgNo) {
            case "1001", "1004", "2001", "2004",
                 "1101", "1102", "1103", "1104",
                 "2102", "2103", "2104",
                 "3000", "3007", "3009", "3101", "3102",
                 "3105", "3107", "3109", "3112", "3116" -> true;
            default -> false;
        };
    }
}
