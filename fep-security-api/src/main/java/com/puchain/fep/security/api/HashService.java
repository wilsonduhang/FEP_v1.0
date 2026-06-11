package com.puchain.fep.security.api;

/**
 * 国密 SM3 摘要服务（GB/T 32905-2016）。
 *
 * <p>报文携带文件场景的 {@code FileContentHash}（PRD §3.3.2 / §3.2.2，Hex ..64）算法基座。
 * 纯哈希无密钥材料，always-on 装配（不随 {@code fep.security.provider} 切换）。
 * 报文加签/验签流程的 FileContentHash 填充与比对 wiring 属 S2b。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface HashService {

    /**
     * 计算 SM3 摘要。
     *
     * @param data 待摘要数据，非 null（可为空数组）
     * @return 64 字符小写十六进制摘要（与 DataType.xsd Hex 类型兼容）
     * @throws IllegalArgumentException data 为 null
     */
    String sm3Hex(byte[] data);
}
