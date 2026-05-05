package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.SignService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * P5 T5 — 出站报文 SM3withSM2 加签适配器（PRD v1.3 §3.1 报文鉴权）。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>从 {@link KeyService#getSignPrivateKey()} 拉取 SM2 签名私钥（PKCS#8）</li>
 *   <li>调用 {@link SignService#sign(byte[], byte[])} 计算 Base64 签名值</li>
 *   <li>在 CFX 报文 {@code </CFX>} 闭合标签前嵌入 {@code <!-- signature: BASE64 -->} 注释</li>
 *   <li>任意失败抛 {@link FepErrorCode#OUTBOUND_5103_SIGN_FAILURE}（保留 cause）</li>
 * </ol>
 *
 * <p><strong>⛔ Mode E 边界:</strong> 本适配器仅做编排（orchestration），不直接实现任何
 * 国密密码学原语。所有 SM2/SM3 计算委托给 {@link SignService} 接口；私钥来源委托给
 * {@link KeyService} 接口。两个接口的真实实现位于 {@code fep-security-impl}，由
 * ③ 安全工程师人工编写（AI 禁入）。</p>
 *
 * <p><strong>注释嵌入策略:</strong> 使用 {@code lastIndexOf("</CFX>")} 而非
 * {@code indexOf}，避免畸形/嵌套报文中靠前的子串干扰；签名注释始终插入在最后一个
 * {@code </CFX>}（即文档真正的根闭合标签）之前。</p>
 *
 * <p><strong>FR-ID:</strong> FR-MSG-OUTBOUND-SIGN</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundSignAdapter {

    private static final String CFX_CLOSING_TAG = "</CFX>";

    private final SignService signService;
    private final KeyService keyService;

    /**
     * 构造函数注入 SM2 签名服务与密钥服务。
     *
     * @param signService SM2 签名服务（{@link SignService} 接口，真实实现 ⛔ Mode E）
     * @param keyService  密钥服务（{@link KeyService} 接口，真实实现 ⛔ Mode E）
     * @throws NullPointerException 如任一参数为 null
     */
    public OutboundSignAdapter(final SignService signService, final KeyService keyService) {
        this.signService = Objects.requireNonNull(signService, "signService 不可为 null");
        this.keyService = Objects.requireNonNull(keyService, "keyService 不可为 null");
    }

    /**
     * 在 CFX 报文 {@code </CFX>} 闭合标签前嵌入 {@code <!-- signature: BASE64 -->} 注释。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>调 {@link KeyService#getSignPrivateKey()} 取私钥（PKCS#8 byte[]）</li>
     *   <li>对 UTF-8 编码的 xml 字节调 {@link SignService#sign(byte[], byte[])}</li>
     *   <li>用 {@code lastIndexOf("</CFX>")} 定位闭合标签位置</li>
     *   <li>在该位置之前插入注释；返回拼接后的新 XML 字符串</li>
     * </ol>
     *
     * <p>失败模式（统一抛 {@link FepErrorCode#OUTBOUND_5103_SIGN_FAILURE}）：</p>
     * <ul>
     *   <li>输入缺失 {@code </CFX>} → 业务异常，消息 "无法定位 </CFX> 闭合标签"</li>
     *   <li>{@link KeyService}/{@link SignService} 抛任意 {@link Exception} → 包装为业务异常，
     *       cause 链保留</li>
     *   <li>已是 {@link FepBusinessException} → 直接透传，避免双重包装（保留原错误码）</li>
     * </ul>
     *
     * @param xml 完整的 CFX XML 报文字符串（必须以 {@code </CFX>} 结尾）
     * @return 嵌入签名注释后的 XML 字符串
     * @throws FepBusinessException 加签失败（错误码 OUTBOUND_5103）
     */
    public String embedSignatureAsComment(final String xml) {
        try {
            final byte[] privateKey = keyService.getSignPrivateKey();
            final String signature = signService.sign(xml.getBytes(StandardCharsets.UTF_8), privateKey);
            final String comment = "<!-- signature: " + signature + " -->";
            final int idx = xml.lastIndexOf(CFX_CLOSING_TAG);
            if (idx < 0) {
                throw new FepBusinessException(FepErrorCode.OUTBOUND_5103_SIGN_FAILURE,
                        "无法定位 </CFX> 闭合标签");
            }
            return xml.substring(0, idx) + comment + xml.substring(idx);
        } catch (FepBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new FepBusinessException(FepErrorCode.OUTBOUND_5103_SIGN_FAILURE, "加签失败", e);
        }
    }
}
