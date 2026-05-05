package com.puchain.fep.converter.wire;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.model.RequestBusinessHead;

import java.util.Objects;

/**
 * Wire-shape 描述符 — 单一真相源描述某 msgNo 报文在 wire 上的 head 元素名 + head 类型 +
 * 是否要求 ResultCode（PRD v1.3 §3.2 报文结构 + §4.6 报文方向）。
 *
 * <p>P5 T3：消除 inbound {@code BatchMessageProcessorService.wrapBodyInCfx} 与 outbound
 * {@code OutboundCfxEnvelopeBuilder} 两条 head 元素拼装路径分歧（前者历史 hardcoded
 * {@code "RealHead" + msgNo} 仅 3009 正确，其余 7/8 错为 BatchHead{msgNo}）。两者
 * 现统一通过 {@link OutboundWireShapeDispatcher#describeFor} 拿描述符。</p>
 *
 * <p>3 类 wire-shape:</p>
 * <ul>
 *   <li>{@code RealHead3009} + {@link RequestBusinessHead}（仅 3009）</li>
 *   <li>{@code BatchHead3101} + ResponseBusinessHead 含 Result（仅 3101）</li>
 *   <li>{@code BatchHead{msgNo}} + RequestBusinessHead（其余 6: 3102/3105/3107/3109/3112/3116）</li>
 * </ul>
 *
 * @param headElementName  XML head 元素名（如 {@code "RealHead3009"} / {@code "BatchHead3101"}）
 * @param headClass        head POJO 类（{@link RequestBusinessHead} 或其子类
 *                         {@code ResponseBusinessHead}）
 * @param requiresResultCode 是否要求 head 含 ResultCode（仅 3101 true）
 *
 * @author FEP Team
 * @since 1.0.0
 */
public record WireShapeDescriptor(
        String headElementName,
        Class<? extends RequestBusinessHead> headClass,
        boolean requiresResultCode) {

    /**
     * Compact constructor — 验证 {@code headElementName} 与 {@code headClass} 非空。
     *
     * @throws NullPointerException 任一引用参数为 {@code null}
     */
    public WireShapeDescriptor {
        Objects.requireNonNull(headElementName, "headElementName");
        Objects.requireNonNull(headClass, "headClass");
    }

    /**
     * 反射构造一个 head 实例。供 {@code OutboundCfxEnvelopeBuilder} 与
     * {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher} 调用方共享
     * （消除 P5 T4 quality 非阻塞 #2 的两份重复反射代码）。
     *
     * @return 新构造的 head 实例（{@link RequestBusinessHead} 或子类）
     * @throws FepBusinessException 反射失败（无 public 无参 ctor / ctor 抛异常 等），
     *                              错误码 {@link FepErrorCode#OUTBOUND_5101_ENVELOPE_BUILD_FAILURE}
     */
    public RequestBusinessHead newHeadInstance() {
        try {
            return headClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5101_ENVELOPE_BUILD_FAILURE,
                    "无法实例化 head 类: " + headClass.getName(), e);
        }
    }
}
