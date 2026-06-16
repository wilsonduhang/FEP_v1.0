package com.puchain.fep.converter.sign;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.common.util.TextUtil;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.converter.xml.SignatureCommentCodec;
import com.puchain.fep.converter.xml.SignatureRangeExtractor;
import com.puchain.fep.security.api.MessageSignPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 报文加签编排。参见 PRD v1.3 §3.3.2 加签流程。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>通过 {@link SignatureRangeExtractor} 提取签名范围</li>
 *   <li>以 UTF-8 编码为 {@code byte[]}</li>
 *   <li>调用 {@link MessageSignPort#sign(byte[])} 获取 Base64 签名</li>
 *   <li>通过 {@link SignatureCommentCodec} 拼接末端 {@code <!--SIG-->} 注释</li>
 * </ol>
 *
 * <p>⛔ 本类不直接执行 SM2 算法、不持有密钥字节（GM S2b 形态 C-ev）：加签私钥由
 * {@link MessageSignPort} 经 KeyService 单源取，形态依赖（进程内 BC / 外部 1818）被 port 隔离。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class MessageSigner {

    private static final Logger log = LoggerFactory.getLogger(MessageSigner.class);

    private static final int SIG_LOG_PREFIX_LENGTH = 8;

    private final MessageSignPort messageSignPort;
    private final SignatureRangeExtractor rangeExtractor;
    private final SignatureCommentCodec commentCodec;

    /**
     * 构造加签编排器。
     *
     * @param messageSignPort 报文签验 port（来自 {@code fep-security-api}，形态 C-ev）
     * @param rangeExtractor  签名范围提取器
     * @param commentCodec    签名注释编解码器
     */
    public MessageSigner(final MessageSignPort messageSignPort,
                         final SignatureRangeExtractor rangeExtractor,
                         final SignatureCommentCodec commentCodec) {
        this.messageSignPort = messageSignPort;
        this.rangeExtractor = rangeExtractor;
        this.commentCodec = commentCodec;
    }

    /**
     * 按 PRD §3.3.2 流程加签（私钥经 MessageSignPort 单源，不穿参）。
     *
     * @param xml 待签名的完整 CFX XML（不含签名注释）
     * @return 带末端签名注释的 payload 字符串
     * @throws MessageConverterException CONV_8004 如果签名范围提取失败或 port 返回空
     */
    public String sign(final String xml) {
        String range = rangeExtractor.extract(xml);
        byte[] data = range.getBytes(StandardCharsets.UTF_8);
        String signature = messageSignPort.sign(data);
        if (signature == null || signature.isEmpty()) {
            throw new MessageConverterException(FepErrorCode.CONV_8004, "sign result empty");
        }
        if (log.isDebugEnabled()) {
            // CWE-117 CRLF 清洗，避免日志注入；只记录签名前 8 位防止签名值泄露
            String safeSig = LogSanitizer.sanitize(signature);
            String sigPrefix = TextUtil.truncate(safeSig, SIG_LOG_PREFIX_LENGTH);
            log.debug("sign ok: range={} bytes, sigPrefix={}", data.length, sigPrefix);
        }
        return commentCodec.append(xml, signature);
    }
}
