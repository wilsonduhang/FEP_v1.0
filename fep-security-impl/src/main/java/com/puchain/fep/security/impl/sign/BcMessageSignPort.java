package com.puchain.fep.security.impl.sign;

import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.MessageSignPort;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.security.impl.key.FepSecuritySm2Properties;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link MessageSignPort} B 形态实现（进程内 BouncyCastle，{@code provider=impl} 门控；
 * 形态 C-ev，ADR 2026-06-12）。
 *
 * <p><strong>负责</strong>：报文签名私钥经 {@link KeyService#getSignPrivateKey()} 单源取
 * （v0.2 MAJOR-4，消除双取钥 drift）+ 按 SrcNode 路由对端验签公钥（list 化 try-each 抗轮换）
 * —— 仅密钥选址与委托 {@link SignService}。</p>
 *
 * <p><strong>不负责</strong>：签名范围/注释语义（converter 协议层）；SM2 算法（SignService）；
 * 私钥/对端公钥校验加载（{@code KeyServiceImpl} 启动校验）。无 Spring stereotype，经
 * {@code GmSecurityConfiguration} {@code @Bean} 注册。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class BcMessageSignPort implements MessageSignPort {

    private final SignService signService;
    private final KeyService keyService;
    private final Map<String, List<String>> peerVerifyKeys;

    /**
     * 构造：私钥单源 KeyService + 对端公钥深拷贝（无 live 泄漏，EI_EXPOSE_REP2 红线面）。
     *
     * @param signService SM2 签验原语（S5），非 null
     * @param keyService  报文签名私钥单源（getSignPrivateKey），非 null
     * @param sm2Props    读取 peer-verify-keys（SrcNode → 对端公钥列表），非 null
     */
    public BcMessageSignPort(final SignService signService, final KeyService keyService,
                             final FepSecuritySm2Properties sm2Props) {
        this.signService = Objects.requireNonNull(signService, "signService");
        this.keyService = Objects.requireNonNull(keyService, "keyService");
        Objects.requireNonNull(sm2Props, "sm2Props");
        final Map<String, List<String>> peers = new LinkedHashMap<>();
        sm2Props.getPeerVerifyKeys().forEach((srcNode, hexes) ->
                peers.put(srcNode, hexes == null ? List.of() : List.copyOf(hexes)));
        this.peerVerifyKeys = peers;
    }

    @Override
    public String sign(final byte[] data) {
        // 私钥单源经 KeyService；未配置 msg-sign 段时 KeyServiceImpl 抛 IllegalStateException
        return signService.sign(data, keyService.getSignPrivateKey());
    }

    @Override
    public boolean verify(final byte[] data, final String signatureBase64, final String srcNode) {
        final List<String> pubHexes = peerVerifyKeys.get(srcNode);
        if (pubHexes == null || pubHexes.isEmpty()) {
            throw new IllegalStateException(
                    "no peer verify public key configured for srcNode: " + srcNode);
        }
        // list 化抗轮换：任一已配置公钥验过即真（SM2 验签公开运算，try-each 无安全损失）
        for (final String pubHex : pubHexes) {
            if (signService.verify(data, signatureBase64, HexFormat.of().parseHex(pubHex))) {
                return true;
            }
        }
        return false;
    }
}
