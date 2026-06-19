package com.puchain.fep.security.impl.sign;

import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.MessageSignPort;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.security.impl.key.FepSecuritySm2Properties;
import com.puchain.fep.security.impl.key.PeerVerifyKeyMaps;
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
 * @see "docs/decisions/2026-06-12-s2b-sm2-message-signing-form-decision-gate.md（形态 C-ev 决策门）"
 */
public class BcMessageSignPort implements MessageSignPort {

    private final SignService signService;
    private final KeyService keyService;
    private final Map<String, List<byte[]>> peerVerifyKeys;

    /**
     * 构造：私钥单源 KeyService + 对端公钥构造期预解码（byte[]，EFF-1/5——消除每次 verify 的
     * hex 解析；预解码字节私有 final、verify 只读消费，无 live 泄漏）。
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
        this.peerVerifyKeys = PeerVerifyKeyMaps.decodedCopy(sm2Props.getPeerVerifyKeys());
    }

    @Override
    public String sign(final byte[] data) {
        // 私钥单源经 KeyService；未配置 msg-sign 段时 KeyServiceImpl 抛 IllegalStateException
        return signService.sign(data, keyService.getSignPrivateKey());
    }

    /**
     * 按 SrcNode 路由对端验签公钥并 try-each 验签（PRD §3.3.3）。
     *
     * <p><strong>两类失败语义对比</strong>（QUAL）：① srcNode 未配置任何公钥 → 抛
     * {@link IllegalStateException}（配置缺失是部署错误，须 fail 而非静默放过）；
     * ② 已配置但全部公钥都验不过 → 返回 {@code false}（正常验签否决）。调用方据此区分
     * 「配置问题」与「签名不匹配」。</p>
     *
     * <p>try-each 抗轮换：list 内任一公钥验过即 {@code true}；公钥合法性由
     * {@link com.puchain.fep.security.impl.key.KeyServiceImpl#validateOnStartup()} 启动期
     * fail-fast 保证（曲线点校验），故 verify 期不再校验。</p>
     *
     * @param data            验签原文字节，非 null
     * @param signatureBase64 Base64 裸签（r∥s），非 null
     * @param srcNode         发起方节点代码（路由键），非 null
     * @return 任一已配置公钥验过为 {@code true}，全部验不过为 {@code false}
     * @throws IllegalStateException srcNode 未配置对端验签公钥
     */
    @Override
    public boolean verify(final byte[] data, final String signatureBase64, final String srcNode) {
        final List<byte[]> pubKeys = peerVerifyKeys.get(srcNode);
        if (pubKeys == null || pubKeys.isEmpty()) {
            throw new IllegalStateException(
                    "no peer verify public key configured for srcNode: " + srcNode);
        }
        // list 化抗轮换：任一已配置公钥验过即真（SM2 验签公开运算，try-each 无安全损失）。
        // 公钥已于构造期一次性 parseHex（EFF-1/5），verify 路径不再做 hex 解析。
        for (final byte[] pubKey : pubKeys) {
            if (signService.verify(data, signatureBase64, pubKey)) {
                return true;
            }
        }
        return false;
    }
}
