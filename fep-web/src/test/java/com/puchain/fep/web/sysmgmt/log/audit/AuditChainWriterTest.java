package com.puchain.fep.web.sysmgmt.log.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.security.mock.MockSignService;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 链式写入：串行化无分叉 + 启动恢复 + REQUIRES_NEW + poison 自愈（GM S5 T5）。
 *
 * <p>v0.3 N-2 注记：REQUIRES_NEW 下全部 append 真提交不回滚——本类不加
 * @Transactional，seed 用 JdbcTemplate（自动提交），逐用例 recoverChainTail +
 * 相对链尾断言隔离，@AfterEach 清理本类行（logId 前缀 t5wr）防共享 H2 污染。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
class AuditChainWriterTest {

    @Autowired
    private SysOperationLogRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AuditIntegrityService integrity;
    private AuditChainWriter writer;
    private long baseSeq;

    @BeforeEach
    void setUp() {
        // 装配块收编 AuditIntegrityTestSupport（池⑥：fixture 同步义务 2→1 处）
        this.integrity = AuditIntegrityTestSupport.newIntegrityService(new MockSignService());
        this.writer = new AuditChainWriter(repository, integrity,
                transactionManager, new SimpleMeterRegistry());
        writer.recoverChainTail();
        this.baseSeq = repository.findTopBySeqIsNotNullOrderBySeqDesc()
                .map(SysOperationLog::getSeq).orElse(0L);
    }

    @AfterEach
    void cleanUp() {
        // 真提交残留清理（防共享 H2 跨测试污染，红线 shared_h2 教训）
        jdbcTemplate.update("DELETE FROM t_sys_operation_log WHERE log_id LIKE 't5wr%'");
    }

    private static SysOperationLog newRow(final String logId) {
        final SysOperationLog e = new SysOperationLog();
        e.setLogId(logId);
        e.setModule("日志管理");
        e.setOperation(OperationType.QUERY);
        e.setMethod("GET");
        e.setRequestUrl("/api/v1/sys/logs");
        e.setResponseStatus(200);
        e.setIpAddress("127.0.0.1");
        e.setDurationMs(3L);
        e.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return e;
    }

    @Test
    void append_buildsLinkedChainWithRecomputableHashes() {
        final SysOperationLog r1 = newRow("t5wr0000000000000000000000000001");
        final SysOperationLog r2 = newRow("t5wr0000000000000000000000000002");
        writer.append(r1);
        writer.append(r2);
        assertThat(r1.getSeq()).isEqualTo(baseSeq + 1);
        assertThat(r2.getSeq()).isEqualTo(baseSeq + 2);
        assertThat(r2.getPrevHash()).isEqualTo(r1.getHash());
        // hash 重算一致（canonical 同实现）
        final String recomputed = integrity.computeEntryHash(r1.getHash(),
                AuditCanonicalizer.canonicalize(r2, r2.getSeq())
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(r2.getHash()).isEqualTo(recomputed);
        // mock 域签名占位 + keyId 落列
        assertThat(r2.getSignature()).isEqualTo("MOCK_SIGNATURE");
        assertThat(r2.getSignKeyId()).isEqualTo("sm2-audit-v1");
    }

    @Test
    void append_capturesTraceIdFromMdc() {
        MDC.put("traceId", "20260612103005-000042");
        try {
            final SysOperationLog row = newRow("t5wr0000000000000000000000000011");
            writer.append(row);
            assertThat(row.getTraceId()).isEqualTo("20260612103005-000042");
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    void concurrentAppends_produceGaplessUnforkedChain() throws Exception {
        final int threads = 16;
        final int perThread = 20;
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            pool.submit(() -> {
                start.await();
                for (int i = 0; i < perThread; i++) {
                    writer.append(newRow(String.format("t5wrc%03d%02d%022d", tid, i, 0)));
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(120, TimeUnit.SECONDS)).isTrue();
        // 320 行 seq 连续无重复 + 逐行链重算全通
        final List<SysOperationLog> chain = repository
                .findBySeqGreaterThanEqualOrderBySeqAsc(1L,
                        org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(r -> r.getSeq() > baseSeq)
                .toList();
        assertThat(chain).hasSize(threads * perThread);
        String prev = baseSeq == 0 ? AuditIntegrityService.GENESIS_PREV_HASH
                : repository.findBySeqGreaterThanEqualOrderBySeqAsc(1L,
                        org.springframework.data.domain.Pageable.unpaged())
                        .getContent().stream()
                        .filter(r -> r.getSeq() == baseSeq)
                        .findFirst().orElseThrow().getHash();
        long expectedSeq = baseSeq + 1;
        for (final SysOperationLog row : chain) {
            assertThat(row.getSeq()).isEqualTo(expectedSeq);
            assertThat(row.getPrevHash()).isEqualTo(prev);
            final String recomputed = integrity.computeEntryHash(prev,
                    AuditCanonicalizer.canonicalize(row, row.getSeq())
                            .getBytes(StandardCharsets.UTF_8));
            assertThat(row.getHash()).isEqualTo(recomputed);
            prev = row.getHash();
            expectedSeq++;
        }
    }

    @Test
    void recoverChainTail_resumesFromPersistedTail() {
        writer.append(newRow("t5wr0000000000000000000000000021"));
        final SysOperationLog tail = newRow("t5wr0000000000000000000000000022");
        writer.append(tail);
        // 模拟重启：新 writer 实例恢复链尾
        final AuditChainWriter restarted = new AuditChainWriter(repository, integrity,
                transactionManager, new SimpleMeterRegistry());
        restarted.recoverChainTail();
        final SysOperationLog next = newRow("t5wr0000000000000000000000000023");
        restarted.append(next);
        assertThat(next.getSeq()).isEqualTo(tail.getSeq() + 1);
        assertThat(next.getPrevHash()).isEqualTo(tail.getHash());
    }

    @Test
    void seqCollision_selfHealsViaRecoverAndRethrows() {
        // 预占 seq=base+1（模拟意外多实例写入），完整假行（hash 须非 null 供自愈恢复读取）
        jdbcTemplate.update("INSERT INTO t_sys_operation_log (log_id, module, operation, "
                        + "method, request_url, response_status, ip_address, duration_ms, "
                        + "create_time, seq, prev_hash, hash, signature, sign_key_id) "
                        + "VALUES (?, '日志管理', 'QUERY', 'GET', '/x', 200, '127.0.0.1', 1, "
                        + "CURRENT_TIMESTAMP, ?, ?, ?, 'MOCK_SIGNATURE', 'sm2-audit-v1')",
                "t5wr0000000000000000000000000031", baseSeq + 1,
                "0".repeat(64), "f".repeat(64));
        // writer 内存链尾仍 baseSeq → append 撞 uk_audit_seq → 自愈 + rethrow
        assertThatThrownBy(() -> writer.append(newRow("t5wr0000000000000000000000000032")))
                .isInstanceOf(DataIntegrityViolationException.class);
        // 自愈后链尾 = base+1（假行），下一次 append 在正确链尾继续
        final SysOperationLog next = newRow("t5wr0000000000000000000000000033");
        writer.append(next);
        assertThat(next.getSeq()).isEqualTo(baseSeq + 2);
        assertThat(next.getPrevHash()).isEqualTo("f".repeat(64));
    }
}
