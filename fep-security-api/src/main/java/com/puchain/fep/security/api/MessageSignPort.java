package com.puchain.fep.security.api;

/**
 * 报文签验 port（形态 C-ev 适配缝，ADR 2026-06-12）。
 *
 * <p>以"当前节点报文签名身份 + SrcNode 公钥路由"为粒度抽象，<strong>不暴露密钥字节</strong>——
 * B 形态（进程内 BouncyCastle，当前默认）实现读取本地配置私钥；未来 A 形态（外部签名验签
 * 服务器 1818，甲方接口规范到位后）实现同一契约经 Socket 委托，调用方零改动。</p>
 *
 * <p>签名范围语义（首个 {@code <} 至最后 {@code </CFX>}）与 XML 注释存储由 converter 协议层
 * （{@code SignatureRangeExtractor} / {@code SignatureCommentCodec}）负责，本 port 只接收已提取的
 * 签名范围字节，不做范围/注释语义——形态无关，A/B 形态共用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface MessageSignPort {

    /**
     * 以当前节点报文签名私钥对签名范围数据签名。
     *
     * @param data 签名范围 UTF-8 字节（由 converter 协议层提取，本 port 不做范围语义），非 null
     * @return Base64 裸签值（raw r∥s 64 字节）
     * @throws IllegalStateException 报文签名密钥未配置（impl provider）
     */
    String sign(byte[] data);

    /**
     * 按发起方节点代码路由公钥验签（list 化抗轮换：srcNode 任一已配置公钥验过即真）。
     *
     * @param data            签名范围 UTF-8 字节，非 null
     * @param signatureBase64 Base64 裸签值，非 null
     * @param srcNode         报文头 SrcNode（PRD §3.3.3 步骤 1），非 null
     * @return true 验签通过；false 验签失败
     * @throws IllegalStateException srcNode 无已配置公钥（配置缺失，区别于验签失败 false）
     */
    boolean verify(byte[] data, String signatureBase64, String srcNode);
}
