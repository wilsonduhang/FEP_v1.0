package com.puchain.fep.converter.xml;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 签名注释编解码器。参见 PRD v1.3 §3.3.1 / §3.3.2。
 *
 * <p>签名以 XML 注释方式存储在报文末端：{@code <!--Base64签名值-->}。本类提供三个操作：
 * 追加签名注释（{@link #append}）、提取签名注释（{@link #extract}）、剥离末端注释返回
 * 纯 XML 主体（{@link #extractBody}）。</p>
 *
 * <p>本类只做字符串拼接与正则匹配，不做任何密码学运算；签名值的生成与校验由
 * {@code fep-security-api} 的 {@code SignService} 完成。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class SignatureCommentCodec {

    /**
     * 匹配末端 XML 注释，允许尾部空白。
     *
     * <p>捕获组使用 tempered greedy token {@code (?:(?!-->).)*}，保证不会跨越内部
     * {@code -->} 边界，从而正确识别多注释场景下的**最末尾**一个注释。</p>
     */
    private static final Pattern TAIL_COMMENT = Pattern.compile("<!--((?:(?!-->)[\\s\\S])*)-->\\s*$");

    /**
     * 向 XML 末端追加签名注释。
     *
     * @param xml             原始 XML 或签名范围字符串
     * @param base64Signature Base64 编码的签名值
     * @return 带签名注释的完整 payload
     */
    public String append(final String xml, final String base64Signature) {
        return xml + "<!--" + base64Signature + "-->";
    }

    /**
     * 提取 payload 末端签名注释的 Base64 内容。
     *
     * <p>若 payload 含多个注释，只返回**最末尾**那一个（PRD §3.3.1 明确签名位于报文末端）。</p>
     *
     * @param payload 完整 payload（可含也可不含注释）
     * @return 签名 Base64 字符串；无末端注释返回 {@link Optional#empty()}；null 入参返回 empty
     */
    public Optional<String> extract(final String payload) {
        if (payload == null) {
            return Optional.empty();
        }
        Matcher matcher = TAIL_COMMENT.matcher(payload);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * 剥离末端签名注释，返回纯 XML 主体。
     *
     * @param payload 完整 payload
     * @return 不含末端注释的 XML 字符串；若 payload 无末端注释原样返回；null 入参返回 null
     */
    public String extractBody(final String payload) {
        if (payload == null) {
            return null;
        }
        Matcher matcher = TAIL_COMMENT.matcher(payload);
        if (matcher.find()) {
            return payload.substring(0, matcher.start());
        }
        return payload;
    }
}
