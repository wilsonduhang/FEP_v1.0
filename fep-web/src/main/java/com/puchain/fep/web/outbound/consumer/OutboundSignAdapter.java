package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.sign.MessageSigner;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 出站报文 SM3withSM2 加签适配器（PRD v1.3 §3.1 报文鉴权 / §3.2.1 / §3.3.2）。
 *
 * <p><strong>GM S2b（形态 C-ev）瘦身为委托 converter 协议层 {@link MessageSigner}:</strong>
 * 签名范围（首个 {@code <} 至最后 {@code </CFX>}）+ Base64 裸签注释 {@code <!--B64-->} 置于
 * {@code </CFX>} <strong>之后</strong>（PRD §3.2.1 样例）。私钥由 {@code MessageSignPort} 经
 * KeyService 单源取，形态依赖（进程内 BC / 外部 1818）被 port 隔离。</p>
 *
 * <p><strong>修正既有 G1 缺陷:</strong> 旧实现签<em>全文</em>（未走范围提取）+ 注释置 {@code </CFX>}
 * <em>之前</em> + 格式 {@code <!-- signature: B64 -->}（与 PRD 样例及 {@code SignatureCommentCodec}
 * 不一致，对 HNDEMP 真验签必失败）。mock 期未暴露。</p>
 *
 * <p>方法名 {@link #embedSignatureAsComment(String)} 与 {@link FepErrorCode#OUTBOUND_5103_SIGN_FAILURE}
 * 异常面保留——调用方 {@code OutboundQueueRunnerImpl} 零改动。底层 {@code MessageConverterException}
 * （CONV_8004：范围提取失败 / 签名空）统一映射为 OUTBOUND_5103。</p>
 *
 * <p><strong>FR-ID:</strong> FR-MSG-OUTBOUND-SIGN</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundSignAdapter {

    private final MessageSigner messageSigner;

    /**
     * 构造函数注入 converter 协议层加签器。
     *
     * @param messageSigner 报文加签编排器（{@link MessageSigner}，依赖 MessageSignPort）
     * @throws NullPointerException 如参数为 null
     */
    public OutboundSignAdapter(final MessageSigner messageSigner) {
        this.messageSigner = Objects.requireNonNull(messageSigner, "messageSigner 不可为 null");
    }

    /**
     * 委托 {@link MessageSigner#sign(String)} 加签：范围提取 → SM2 裸签 → 末端
     * {@code </CFX><!--B64-->} 注释。
     *
     * @param xml 完整 CFX XML 报文
     * @return 末端嵌入签名注释的 XML 字符串
     * @throws FepBusinessException 加签失败（错误码 OUTBOUND_5103，保留 cause）
     */
    public String embedSignatureAsComment(final String xml) {
        try {
            return messageSigner.sign(xml);
        } catch (FepBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new FepBusinessException(FepErrorCode.OUTBOUND_5103_SIGN_FAILURE, "加签失败", e);
        }
    }
}
