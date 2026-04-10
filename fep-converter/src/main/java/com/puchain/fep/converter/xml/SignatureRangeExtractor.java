package com.puchain.fep.converter.xml;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import org.springframework.stereotype.Component;

/**
 * 签名范围提取器。参见 PRD v1.3 §3.3.1。
 *
 * <p>签名范围定义："从 {@code <?XML} 的 {@code <} 起，到 {@code </CFX>} 的 {@code >} 止"。
 * 任何末端 {@code <!--...-->} 注释（签名本身）不在范围内。</p>
 *
 * <p>本类只做字符串切片，不涉及任何密码学运算；真正的 SM3withSM2 签名/验签由
 * {@code fep-security-api} 的 {@code SignService} 完成，参见 Task 7
 * {@code MessageSigner}/{@code MessageVerifier}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class SignatureRangeExtractor {

    private static final String CFX_CLOSING = "</CFX>";

    /**
     * 提取 XML 中从首个 {@code <} 到最后一个 {@code </CFX>} 之间的子串（含 {@code </CFX>}）。
     *
     * @param xml 完整报文 XML（可以带也可以不带末端签名注释）
     * @return 签名范围字符串
     * @throws MessageConverterException CONV_8004 如果 xml 为 null 或不含 {@code </CFX>}
     */
    public String extract(final String xml) {
        if (xml == null) {
            throw new MessageConverterException(FepErrorCode.CONV_8004, "xml is null");
        }
        int start = xml.indexOf('<');
        int end = xml.lastIndexOf(CFX_CLOSING);
        if (start < 0 || end < 0) {
            throw new MessageConverterException(FepErrorCode.CONV_8004, "missing </CFX>");
        }
        return xml.substring(start, end + CFX_CLOSING.length());
    }
}
