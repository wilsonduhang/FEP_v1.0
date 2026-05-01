package com.puchain.fep.collector.assembler;

import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;

/**
 * 报文组装 Port（forward-declared，由 T7b 实现 {@code DefaultPayloadAssembler}）。
 *
 * <p>负责把上游 {@link CollectionRecord}（原始字段表 + payloadDataType）转换为
 * {@link OutboundMessageEnvelope}（含 messageType / headFields / messageBody / 幂等键）。
 *
 * <p><b>实现约定：</b>
 * <ul>
 *   <li>找不到 {@code payloadDataType} 对应路由 → 抛
 *       {@code FepBusinessException(COLLECT_ASSEMBLE_FAILURE)}</li>
 *   <li>{@code messageBody} 用 {@link Object} 透传 — 因 fep-collector 不依赖
 *       fep-converter 的 {@code CfxBody}（CollectorArchitectureTest R2 守护）</li>
 *   <li>{@code headFields.transitionNo} 必须是 8 位 numeric（PRD §3.2.3）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface PayloadAssembler {

    /**
     * 把采集记录组装为出站报文 envelope。
     *
     * @param record 采集记录（非 null）
     * @return 出站报文 envelope（非 null）
     * @throws com.puchain.fep.common.exception.FepBusinessException
     *         当 payloadDataType 路由缺失 / 必填字段缺失 / 组装失败时
     */
    OutboundMessageEnvelope assemble(CollectionRecord record);
}
