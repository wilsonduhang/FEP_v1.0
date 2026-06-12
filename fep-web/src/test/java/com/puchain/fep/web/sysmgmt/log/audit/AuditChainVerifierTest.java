package com.puchain.fep.web.sysmgmt.log.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.security.impl.audit.AuditIntegrityServiceImpl;
import com.puchain.fep.security.impl.hash.HashServiceImpl;
import com.puchain.fep.security.impl.key.FepSecurityKeyProperties;
import com.puchain.fep.security.impl.key.FepSecuritySm2Properties;
import com.puchain.fep.security.impl.key.KeyServiceImpl;
import com.puchain.fep.security.impl.sign.SignServiceImpl;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 审计链篡改检测：6 断点/正向场景（GM S5 T6）。
 *
 * <p>直构 verifier + 真 SignServiceImpl + 真审计密钥（impl 语义——mock 域
 * MockSignService 恒 true 无法测 SIGNATURE_INVALID）。链行真提交，@AfterEach
 * 清理（logId 前缀 t6vf），逐用例从空链段起（相对基线 = 清理后全删本类行）。</p>
 *
 * <p>注：本类与既有链行共存——为可独立断言，每用例先清空本类行并以
 * 全表无链行为前提跳过（若别测试残留链行则以相对断言兜底）。简化策略：
 * 本类自建 writer 在干净表段写链（先 DELETE 全部链行——dev 共享 H2 中
 * t_sys_operation_log 仅测试数据，清理合规且 @AfterEach 恢复不了别类行属
 * 可接受测试域操作，红线 shared_h2 教训对策为前缀清理 + 全链行清理声明）。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
class AuditChainVerifierTest {

    private static final String PRIV =
            "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8";
    private static final String PUB =
            "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                    + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";

    @Autowired
    private SysOperationLogRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditChainCheckpointRepository checkpointRepository;

    private SimpleMeterRegistry meterRegistry;
    private AuditIntegrityService integrity;
    private AuditChainVerifier verifier;
    private AuditChainWriter writer;

    @BeforeEach
    void setUp() {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        sm2.setAuditActiveKeyId("sm2-audit-v1");
        final FepSecuritySm2Properties.LoginKeyPair pair = new FepSecuritySm2Properties.LoginKeyPair();
        pair.setPrivateKeyHex(PRIV);
        pair.setPublicKeyHex(PUB);
        sm2.getAuditKeys().put("sm2-audit-v1", pair);
        final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
        sm4.setActiveKeyId("sm4-cred-v1");
        sm4.getSm4Keys().put("sm4-cred-v1", "0123456789abcdeffedcba9876543210");
        final KeyServiceImpl keyService = new KeyServiceImpl(sm4, sm2);
        keyService.validateOnStartup();
        this.integrity = new AuditIntegrityServiceImpl(
                new HashServiceImpl(), new SignServiceImpl(), keyService);
        this.meterRegistry = new SimpleMeterRegistry();
        this.verifier = new AuditChainVerifier(repository, integrity,
                checkpointRepository, meterRegistry);
        // 链行清空：verifyChain 从 seq=1 全链校验，须排他链段（声明见类 Javadoc）；
        // checkpoint 同步清空——推进是持久副作用，不得跨用例/跨类泄漏（Plan B1）
        jdbcTemplate.update("DELETE FROM t_sys_operation_log WHERE seq IS NOT NULL");
        jdbcTemplate.update("DELETE FROM audit_chain_checkpoint");
        this.writer = new AuditChainWriter(repository, integrity,
                transactionManager, new SimpleMeterRegistry());
        writer.recoverChainTail();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM t_sys_operation_log WHERE seq IS NOT NULL");
        jdbcTemplate.update("DELETE FROM t_sys_operation_log WHERE log_id LIKE 't6vf%'");
        jdbcTemplate.update("DELETE FROM audit_chain_checkpoint");
    }

    private SysOperationLog appendRow(final String logId) {
        final SysOperationLog e = new SysOperationLog();
        e.setLogId(logId);
        e.setModule("日志管理");
        e.setOperation(OperationType.QUERY);
        e.setMethod("GET");
        e.setRequestUrl("/api/v1/sys/logs");
        e.setResponseStatus(200);
        e.setIpAddress("127.0.0.1");
        e.setDurationMs(2L);
        e.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        writer.append(e);
        return e;
    }

    private void seedChain(final int rows) {
        for (int i = 1; i <= rows; i++) {
            appendRow(String.format("t6vf%028d", i));
        }
    }

    @Test
    void intactChain_reportsIntactWithTotal() {
        seedChain(3);
        final ChainVerifyResult result = verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        assertThat(result.intact()).isTrue();
        assertThat(result.totalChecked()).isEqualTo(3);
        assertThat(result.firstBreakSeq()).isNull();
        assertThat(result.breakType()).isNull();
    }

    @Test
    void tamperedDescription_reportsHashMismatchAtRow() {
        seedChain(3);
        jdbcTemplate.update(
                "UPDATE t_sys_operation_log SET description = 'tampered' WHERE seq = 2");
        final ChainVerifyResult result = verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        assertThat(result.intact()).isFalse();
        assertThat(result.firstBreakSeq()).isEqualTo(2);
        assertThat(result.breakType()).isEqualTo(AuditChainVerifier.BreakType.HASH_MISMATCH);
    }

    @Test
    void tamperedHashColumn_reportsHashMismatchAtRow() {
        seedChain(3);
        jdbcTemplate.update(
                "UPDATE t_sys_operation_log SET hash = ? WHERE seq = 2", "e".repeat(64));
        final ChainVerifyResult result = verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        assertThat(result.intact()).isFalse();
        assertThat(result.firstBreakSeq()).isEqualTo(2);
        assertThat(result.breakType()).isEqualTo(AuditChainVerifier.BreakType.HASH_MISMATCH);
    }

    @Test
    void deletedMiddleRow_reportsGap() {
        seedChain(3);
        jdbcTemplate.update("DELETE FROM t_sys_operation_log WHERE seq = 2");
        final ChainVerifyResult result = verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        assertThat(result.intact()).isFalse();
        assertThat(result.firstBreakSeq()).isEqualTo(3);
        assertThat(result.breakType()).isEqualTo(AuditChainVerifier.BreakType.GAP);
    }

    @Test
    void tamperedSignature_reportsSignatureInvalid() {
        seedChain(2);
        // 合法 Base64 假签名（64 字节全 'A' → r∥s 伪值）
        final String fakeSig = java.util.Base64.getEncoder().encodeToString(new byte[64]);
        jdbcTemplate.update(
                "UPDATE t_sys_operation_log SET signature = ? WHERE seq = 2", fakeSig);
        final ChainVerifyResult result = verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        assertThat(result.intact()).isFalse();
        assertThat(result.firstBreakSeq()).isEqualTo(2);
        assertThat(result.breakType())
                .isEqualTo(AuditChainVerifier.BreakType.SIGNATURE_INVALID);
    }

    @Test
    void unknownSignKeyId_reportsUnknownKey() {
        seedChain(2);
        jdbcTemplate.update(
                "UPDATE t_sys_operation_log SET sign_key_id = 'mock-key-v1' WHERE seq = 2");
        final ChainVerifyResult result = verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        assertThat(result.intact()).isFalse();
        assertThat(result.firstBreakSeq()).isEqualTo(2);
        assertThat(result.breakType()).isEqualTo(AuditChainVerifier.BreakType.UNKNOWN_KEY);
    }

    // ===== EFF-S5-1 checkpoint 增量化 8 场景（Plan T2 验收 1-9） =====

    private AuditChainCheckpoint checkpointRow() {
        return checkpointRepository.findById(AuditChainCheckpoint.SINGLETON_ID).orElseThrow();
    }

    @Test
    void fullIntact_advancesCheckpointAndGauge() {
        seedChain(3);
        final ChainVerifyResult result =
                verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        assertThat(result.intact()).isTrue();
        assertThat(result.mode()).isEqualTo(AuditChainVerifier.VerifyMode.FULL);
        assertThat(result.checkpointSeq()).isEqualTo(3L);
        final AuditChainCheckpoint cp = checkpointRow();
        assertThat(cp.getVerifiedUntilSeq()).isEqualTo(3L);
        assertThat(cp.getAnchorHash()).isEqualTo(jdbcTemplate.queryForObject(
                "SELECT hash FROM t_sys_operation_log WHERE seq = 3", String.class));
        assertThat(cp.getSignKeyId()).isEqualTo("sm2-audit-v1");
        assertThat(meterRegistry.get("fep_audit_chain_checkpoint_seq").gauge().value())
                .isEqualTo(3.0);
    }

    @Test
    void incremental_checksOnlyDeltaAndAdvances() {
        seedChain(3);
        verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        appendRow("t6vf" + "0".repeat(24) + "1004");
        appendRow("t6vf" + "0".repeat(24) + "1005");
        final ChainVerifyResult result =
                verifier.verifyChain(AuditChainVerifier.VerifyMode.INCREMENTAL);
        assertThat(result.intact()).isTrue();
        assertThat(result.totalChecked()).isEqualTo(2);
        assertThat(result.mode()).isEqualTo(AuditChainVerifier.VerifyMode.INCREMENTAL);
        assertThat(result.checkpointSeq()).isEqualTo(5L);
        assertThat(checkpointRow().getVerifiedUntilSeq()).isEqualTo(5L);
    }

    @Test
    void incremental_withoutCheckpoint_degradesToFullScan() {
        seedChain(3);
        final ChainVerifyResult result =
                verifier.verifyChain(AuditChainVerifier.VerifyMode.INCREMENTAL);
        assertThat(result.intact()).isTrue();
        assertThat(result.totalChecked()).isEqualTo(3);
        assertThat(result.checkpointSeq()).isEqualTo(3L);
    }

    @Test
    void truncatedTail_reportsTruncationAndKeepsCheckpoint() {
        seedChain(5);
        verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        jdbcTemplate.update("DELETE FROM t_sys_operation_log WHERE seq IN (4, 5)");
        final ChainVerifyResult result =
                verifier.verifyChain(AuditChainVerifier.VerifyMode.INCREMENTAL);
        assertThat(result.intact()).isFalse();
        assertThat(result.breakType())
                .isEqualTo(AuditChainVerifier.BreakType.TRUNCATION);
        assertThat(result.firstBreakSeq()).isEqualTo(5L);
        // broken 不推进：锚保持原值（AC7）
        assertThat(checkpointRow().getVerifiedUntilSeq()).isEqualTo(5L);
    }

    @Test
    void tamperedAnchorRowHash_reportsHashMismatchAtAnchor() {
        seedChain(3);
        verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        jdbcTemplate.update(
                "UPDATE t_sys_operation_log SET hash = ? WHERE seq = 3", "f".repeat(64));
        final ChainVerifyResult result =
                verifier.verifyChain(AuditChainVerifier.VerifyMode.INCREMENTAL);
        assertThat(result.intact()).isFalse();
        assertThat(result.breakType())
                .isEqualTo(AuditChainVerifier.BreakType.HASH_MISMATCH);
        assertThat(result.firstBreakSeq()).isEqualTo(3L);
    }

    @Test
    void deletedAnchorRowWithLaterRows_reportsTruncation() {
        seedChain(3);
        verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        appendRow("t6vf" + "0".repeat(24) + "2004");
        appendRow("t6vf" + "0".repeat(24) + "2005");
        jdbcTemplate.update("DELETE FROM t_sys_operation_log WHERE seq = 3");
        final ChainVerifyResult result =
                verifier.verifyChain(AuditChainVerifier.VerifyMode.INCREMENTAL);
        assertThat(result.intact()).isFalse();
        assertThat(result.breakType())
                .isEqualTo(AuditChainVerifier.BreakType.TRUNCATION);
        assertThat(result.firstBreakSeq()).isEqualTo(3L);
    }

    @Test
    void forgedCheckpointAdvance_reportsCheckpointInvalid() {
        seedChain(3);
        verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        appendRow("t6vf" + "0".repeat(24) + "3004");
        appendRow("t6vf" + "0".repeat(24) + "3005");
        // 攻击者前移锚至未验 seq=5：anchor_hash 填真实行 hash + 复制该行 signature 列
        //（行签名输入=64-hex hash；checkpoint 验签输入=域分隔串 → 不可复用，抉择④）
        jdbcTemplate.update(
                "UPDATE audit_chain_checkpoint SET verified_until_seq = 5,"
                        + " anchor_hash = (SELECT hash FROM t_sys_operation_log WHERE seq = 5),"
                        + " checkpoint_signature ="
                        + " (SELECT signature FROM t_sys_operation_log WHERE seq = 5)");
        final ChainVerifyResult result =
                verifier.verifyChain(AuditChainVerifier.VerifyMode.INCREMENTAL);
        assertThat(result.intact()).isFalse();
        assertThat(result.breakType())
                .isEqualTo(AuditChainVerifier.BreakType.CHECKPOINT_INVALID);
        assertThat(result.firstBreakSeq()).isEqualTo(5L);
    }

    @Test
    void zeroNewRows_intactZeroWithoutResigning() {
        seedChain(3);
        verifier.verifyChain(AuditChainVerifier.VerifyMode.FULL);
        final AuditChainCheckpoint before = checkpointRow();
        final ChainVerifyResult result =
                verifier.verifyChain(AuditChainVerifier.VerifyMode.INCREMENTAL);
        assertThat(result.intact()).isTrue();
        assertThat(result.totalChecked()).isZero();
        assertThat(result.checkpointSeq()).isEqualTo(3L);
        final AuditChainCheckpoint after = checkpointRow();
        // 不重签不推进（Plan C5）：签名与推进时刻原值保持
        assertThat(after.getCheckpointSignature()).isEqualTo(before.getCheckpointSignature());
        assertThat(after.getVerifiedAt()).isEqualTo(before.getVerifiedAt());
    }
}
