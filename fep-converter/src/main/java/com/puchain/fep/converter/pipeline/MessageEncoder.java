package com.puchain.fep.converter.pipeline;

import com.puchain.fep.converter.compress.ZipBase64Compressor;
import com.puchain.fep.converter.encrypt.MessageEncryptor;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.sign.MessageSigner;
import com.puchain.fep.converter.xml.XmlCodec;
import org.springframework.stereotype.Component;

/**
 * 出站编码流水线。参见 PRD v1.3 §3.2 / §3.3 / §3.4。
 *
 * <p>顺序严格遵循 PRD §3.4.2："如同时压缩+加密，应先压缩再加密"。签名在
 * 压缩之前执行，因为 PRD §3.3.1 签名范围基于明文 XML
 * （{@code <?XML} 到 {@code </CFX>}）。</p>
 *
 * <pre>
 * marshal(CfxMessage) → sign(xml) → compress(xml) → encrypt(xml)
 * </pre>
 *
 * <p>依赖数 4（xmlCodec / signer / compressor / encryptor），在 ≤7 的上限内。
 * 本类仅负责编排，不含任何密码学实现。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class MessageEncoder {

    private final XmlCodec xmlCodec;
    private final MessageSigner signer;
    private final ZipBase64Compressor compressor;
    private final MessageEncryptor encryptor;

    /**
     * 构造编码流水线。
     *
     * @param xmlCodec XML 序列化器
     * @param signer 报文签名器
     * @param compressor ZIP+Base64 压缩器
     * @param encryptor SM4 加密器
     */
    public MessageEncoder(final XmlCodec xmlCodec,
                          final MessageSigner signer,
                          final ZipBase64Compressor compressor,
                          final MessageEncryptor encryptor) {
        this.xmlCodec = xmlCodec;
        this.signer = signer;
        this.compressor = compressor;
        this.encryptor = encryptor;
    }

    /**
     * 编码 CfxMessage 为 payload 字符串。
     *
     * @param message CfxMessage 领域对象
     * @param opts 流水线选项
     * @return 编码结果（含最终 payload 和 zip / encrypt 标志）
     */
    public EncodeResult encode(final CfxMessage message, final MessagePipelineOptions opts) {
        String payload = xmlCodec.marshal(message);
        if (opts.isSign()) {
            payload = signer.sign(payload);
        }
        if (opts.isZip()) {
            payload = compressor.compress(payload);
        }
        if (opts.isEncrypt()) {
            payload = encryptor.encrypt(payload, opts.getEncryptKey());
        }
        return new EncodeResult(payload, opts.isZip(), opts.isEncrypt());
    }
}
