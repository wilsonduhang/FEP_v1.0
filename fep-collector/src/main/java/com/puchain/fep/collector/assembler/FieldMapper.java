package com.puchain.fep.collector.assembler;

import java.util.Map;

/**
 * 报文体字段映射 SPI（Plan §T7b §1）。
 *
 * <p>每个 messageType 一个实现，把行内/外部系统提供的 {@code Map<String, Object>} 字段集
 * 转换为对应 messageType 的 Body POJO（如 {@code ContractInfo3101}）。
 *
 * <p><b>返回类型为 {@link Object} 的设计取舍：</b>
 * <ul>
 *   <li>fep-collector 不允许依赖 fep-converter（CollectorArchitectureTest R2 守护），
 *       因此无法以 {@code CfxBody} 作为返回类型。</li>
 *   <li>下游 P5+ 队列消费侧根据 {@code messageType} 强转为对应 Body 类型。</li>
 * </ul>
 *
 * <p><b>实现约定：</b>
 * <ul>
 *   <li>必填字段（XSD {@code required=true}）缺失 → 抛
 *       {@link com.puchain.fep.common.exception.FepBusinessException} +
 *       {@link com.puchain.fep.common.domain.FepErrorCode#COLLECT_ASSEMBLE_FAILURE}，
 *       消息形如 {@code "missing required field for <msgNo>: <fieldName>"}。</li>
 *   <li>可选字段缺失 → 跳过 setter 调用即可。</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface FieldMapper {

    /**
     * 把原始字段集映射为指定 messageType 的 Body POJO。
     *
     * @param rawData 原始字段数据（非 null；空 Map 允许，但通常会因必填字段缺失抛业务异常）
     * @return Body POJO（非 null；具体类型由实现决定，下游按 messageType 强转）
     * @throws com.puchain.fep.common.exception.FepBusinessException
     *         必填字段缺失等业务校验失败
     */
    Object toMessageBody(Map<String, Object> rawData);
}
