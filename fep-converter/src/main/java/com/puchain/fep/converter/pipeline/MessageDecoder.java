package com.puchain.fep.converter.pipeline;

import com.puchain.fep.converter.compress.ZipBase64Compressor;
import com.puchain.fep.converter.encrypt.MessageEncryptor;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.sign.MessageVerifier;
import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.XmlCodec;
import org.springframework.stereotype.Component;

/**
 * 入站解码流水线。严格反向：decrypt → decompress → verify → strip-comment → unmarshal。
 *
 * <p>签名注释的剥离统一交给 {@link SignatureCommentCodec#extractBody(String)}，
 * 避免手工 {@code lastIndexOf("<!--")} 漏掉非贴尾注释或误伤 XML 内嵌注释
 * （Plan v1.1 🟡#2 修复）。</p>
 *
 * <p>依赖数 5（xmlCodec / verifier / compressor / encryptor / commentCodec），
 * 仍在 ≤7 的上限内。本类仅负责编排，不含任何密码学实现。</p>
 *
 * <p>验签失败时仍会继续 unmarshal 并返回 CfxMessage，{@link DecodeResult#isVerified()}
 * 标记 {@code false}，由业务层根据场景决定是否拒绝处理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class MessageDecoder {

    private final XmlCodec xmlCodec;
    private final MessageVerifier verifier;
    private final ZipBase64Compressor compressor;
    private final MessageEncryptor encryptor;
    private final SignatureCommentCodec commentCodec;

    /**
     * 构造解码流水线。
     *
     * @param xmlCodec XML 反序列化器
     * @param verifier 报文验签器
     * @param compressor ZIP+Base64 解压器
     * @param encryptor SM4 解密器
     * @param commentCodec 签名注释编解码器
     */
    public MessageDecoder(final XmlCodec xmlCodec,
                          final MessageVerifier verifier,
                          final ZipBase64Compressor compressor,
                          final MessageEncryptor encryptor,
                          final SignatureCommentCodec commentCodec) {
        this.xmlCodec = xmlCodec;
        this.verifier = verifier;
        this.compressor = compressor;
        this.encryptor = encryptor;
        this.commentCodec = commentCodec;
    }

    /**
     * 解码 payload 为 CfxMessage。
     *
     * @param payload 入站 payload 字符串
     * @param opts 流水线选项（zip / encrypt / sign 应与 payload 实际属性匹配）
     * @return 解码结果（含 CfxMessage 和验签状态）
     */
    public DecodeResult decode(final String payload, final MessagePipelineOptions opts) {
        String current = payload;
        if (opts.isEncrypt()) {
            current = encryptor.decrypt(current, opts.getEncryptKey());
        }
        if (opts.isZip()) {
            current = compressor.decompress(current);
        }
        boolean verified = true;
        if (opts.isSign()) {
            verified = verifier.verify(current, opts.getSignPublicKey());
        }
        final String body = commentCodec.extractBody(current);
        final CfxMessage msg = xmlCodec.unmarshal(body);
        return new DecodeResult(msg, verified);
    }
}
