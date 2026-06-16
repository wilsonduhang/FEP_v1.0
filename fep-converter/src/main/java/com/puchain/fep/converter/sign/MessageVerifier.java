package com.puchain.fep.converter.sign;

import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.SignatureRangeExtractor;
import com.puchain.fep.security.api.MessageSignPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 报文验签编排。参见 PRD v1.3 §3.3.3 验签流程。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>提取末端签名注释，无注释返回 {@code false}</li>
 *   <li>去除末端注释后提取签名范围</li>
 *   <li>以 UTF-8 编码为 {@code byte[]}</li>
 *   <li>调用 {@link MessageSignPort#verify(byte[], String, String)} 按 SrcNode 路由公钥验签</li>
 * </ol>
 *
 * <p>⛔ 本类不直接执行 SM2 验签算法、不持有公钥字节（GM S2b 形态 C-ev）：验签公钥按
 * {@code srcNode} 经 {@link MessageSignPort} 路由（PRD §3.3.3 步骤 1），形态依赖被 port 隔离。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class MessageVerifier {

    private static final Logger log = LoggerFactory.getLogger(MessageVerifier.class);

    private final MessageSignPort messageSignPort;
    private final SignatureRangeExtractor rangeExtractor;
    private final SignatureCommentCodec commentCodec;

    /**
     * 构造验签编排器。
     *
     * @param messageSignPort 报文签验 port（来自 {@code fep-security-api}，形态 C-ev）
     * @param rangeExtractor  签名范围提取器
     * @param commentCodec    签名注释编解码器
     */
    public MessageVerifier(final MessageSignPort messageSignPort,
                           final SignatureRangeExtractor rangeExtractor,
                           final SignatureCommentCodec commentCodec) {
        this.messageSignPort = messageSignPort;
        this.rangeExtractor = rangeExtractor;
        this.commentCodec = commentCodec;
    }

    /**
     * 按 PRD §3.3.3 流程验签（公钥按 srcNode 路由，不穿参）。
     *
     * @param payload 带末端签名注释的完整 payload
     * @param srcNode 发起方节点代码（公钥路由键，PRD §3.3.3 步骤 1）
     * @return {@code true} 验签通过；无签名注释或验签失败返回 {@code false}
     * @throws IllegalStateException srcNode 无已配置对端公钥（配置缺失，区别于验签失败）
     */
    public boolean verify(final String payload, final String srcNode) {
        Optional<String> signature = commentCodec.extract(payload);
        if (signature.isEmpty()) {
            log.debug("verify: no signature comment found, returning false");
            return false;
        }
        String body = commentCodec.extractBody(payload);
        String range = rangeExtractor.extract(body);
        return messageSignPort.verify(
                range.getBytes(StandardCharsets.UTF_8), signature.get(), srcNode);
    }
}
