package com.puchain.fep.web.sysmgmt.log.audit;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.security.impl.audit.AuditIntegrityServiceImpl;
import com.puchain.fep.security.impl.hash.HashServiceImpl;
import com.puchain.fep.security.impl.key.FepSecurityKeyProperties;
import com.puchain.fep.security.impl.key.FepSecuritySm2Properties;
import com.puchain.fep.security.impl.key.KeyServiceImpl;
import com.puchain.fep.web.support.Sm2WebTestVectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 审计完整性测试共享 util（EFF-S5-1 T4，S5 Simplify 池②⑥ drain）：
 * 完整性原语装配（GB/T 密钥对见 {@link com.puchain.fep.web.support.Sm2WebTestVectors}）
 * + 链/checkpoint 清场。
 *
 * <p>装配块原在 Writer/Verifier 两测试 setUp 重复（fixture 同步义务 2→1 处）；
 * 清场块原在 Verifier/Controller 测试三处内联。</p>
 */
public final class AuditIntegrityTestSupport {

    private AuditIntegrityTestSupport() {
    }

    /**
     * 装配完整性原语（真 HashServiceImpl + 调用方指定 SignService + GB/T 审计密钥段）。
     *
     * @param signService 真 SignServiceImpl（验签语义用例）或 MockSignService（恒 true 用例）
     * @return 完整性原语
     */
    public static AuditIntegrityService newIntegrityService(final SignService signService) {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        sm2.setAuditActiveKeyId("sm2-audit-v1");
        final FepSecuritySm2Properties.LoginKeyPair pair =
                new FepSecuritySm2Properties.LoginKeyPair();
        pair.setPrivateKeyHex(Sm2WebTestVectors.GBT_PRIV);
        pair.setPublicKeyHex(Sm2WebTestVectors.GBT_PUB);
        sm2.getAuditKeys().put("sm2-audit-v1", pair);
        final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
        sm4.setActiveKeyId("sm4-cred-v1");
        sm4.getSm4Keys().put("sm4-cred-v1", "0123456789abcdeffedcba9876543210");
        final KeyServiceImpl keyService = new KeyServiceImpl(sm4, sm2);
        keyService.validateOnStartup();
        return new AuditIntegrityServiceImpl(new HashServiceImpl(), signService, keyService);
    }

    /**
     * 排他链段清场：清链行 + 清 checkpoint（持久副作用不得跨用例/跨类泄漏，Plan B1）
     * + context 单例 writer 重锚（防内存链尾 stale 致后续写入 GAP flake）。
     *
     * @param jdbcTemplate  共享 H2 直改入口
     * @param contextWriter Spring context 单例 AuditChainWriter（test 直构 writer 无需传其自身）
     */
    public static void resetChain(final JdbcTemplate jdbcTemplate, final AuditChainWriter contextWriter) {
        jdbcTemplate.update("DELETE FROM t_sys_operation_log WHERE seq IS NOT NULL");
        jdbcTemplate.update("DELETE FROM audit_chain_checkpoint");
        contextWriter.recoverChainTail();
    }
}
