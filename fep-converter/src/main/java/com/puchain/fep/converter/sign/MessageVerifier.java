package com.puchain.fep.converter.sign;

import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.SignatureRangeExtractor;
import com.puchain.fep.security.api.SignService;
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
 *   <li>调用 {@link SignService#verify(byte[], String, byte[])} 验签</li>
 * </ol>
 *
 * <p>⛔ 本类不直接执行 SM2 验签算法，所有原语通过 security-api 调用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class MessageVerifier {

    private static final Logger log = LoggerFactory.getLogger(MessageVerifier.class);

    private final SignService signService;
    private final SignatureRangeExtractor rangeExtractor;
    private final SignatureCommentCodec commentCodec;

    /**
     * 构造验签编排器。
     *
     * @param signService    SM2 验签服务（来自 {@code fep-security-api}）
     * @param rangeExtractor 签名范围提取器
     * @param commentCodec   签名注释编解码器
     */
    public MessageVerifier(final SignService signService,
                           final SignatureRangeExtractor rangeExtractor,
                           final SignatureCommentCodec commentCodec) {
        this.signService = signService;
        this.rangeExtractor = rangeExtractor;
        this.commentCodec = commentCodec;
    }

    /**
     * 按 PRD §3.3.3 流程验签。
     *
     * @param payload   带末端签名注释的完整 payload
     * @param publicKey SM2 公钥字节
     * @return {@code true} 验签通过；无签名注释或验签失败返回 {@code false}
     */
    public boolean verify(final String payload, final byte[] publicKey) {
        Optional<String> signature = commentCodec.extract(payload);
        if (signature.isEmpty()) {
            log.debug("verify: no signature comment found, returning false");
            return false;
        }
        String body = commentCodec.extractBody(payload);
        String range = rangeExtractor.extract(body);
        return signService.verify(range.getBytes(StandardCharsets.UTF_8), signature.get(), publicKey);
    }
}
