package com.puchain.fep.converter.model;

/**
 * 携带业务流水号的 CFX 报文 Body POJO mixin 接口。
 *
 * <p>实现本接口表明该 Body 在业务层暴露 8 位 SerialNo 字段（PRD §3.1 报文流水号），
 * inbound dispatcher 据此发布 {@code InboundMessageProcessedEvent.serialNo}。</p>
 *
 * <p><b>类型守卫语义</b>: dispatcher 通过 {@code instanceof SerialNoBearing} 模式匹配
 * 替代 {@link java.lang.reflect.Method} 反射调用（E-3 重构，2026-05-08）。
 * 仅由 {@code InboundMessageDispatcher.BODY_TYPE_REGISTRY} 注册的顶层 Body POJO
 * 实现本接口；嵌套子结构（如 SignInfo / DbInfo / ContractInfo）不实现。</p>
 *
 * <p><b>实现方契约</b>:</p>
 * <ul>
 *   <li>正常情况: 返回 XSD 必填字段 SerialNo 的 8 位业务流水号</li>
 *   <li>JAXB unmarshal 后 SerialNo 字段未填充: 返回 {@code null} —
 *       dispatcher 将 fallback 到 transitionNo</li>
 *   <li>空串: dispatcher 视同 fallback（同 null 处理）</li>
 * </ul>
 *
 * <p><b>使用方</b>: {@code com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher}</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@FunctionalInterface
public interface SerialNoBearing {

    /**
     * 返回 Body POJO 携带的业务流水号。
     *
     * @return 8 位业务流水号；JAXB 字段未填充时可为 {@code null} 或空串
     *         （由 dispatcher fallback 处理）
     */
    String getSerialNo();
}
