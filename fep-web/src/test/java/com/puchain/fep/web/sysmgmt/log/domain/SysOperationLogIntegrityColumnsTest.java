package com.puchain.fep.web.sysmgmt.log.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * V36 完整性列 schema + entity 映射 + 链读 repository 方法（GM S5 T4）。
 *
 * <p>@Transactional 末尾回滚隔离；uk_audit_seq 冲突经 saveAndFlush 强制触发。
 * <strong>seq 全部相对链尾 base</strong>（红线 shared_h2 教训：CI 全量时其他
 * @SpringBootTest 经 @OperationLog 切面已真实提交链行，绝对 seq 字面值必撞唯一键）。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class SysOperationLogIntegrityColumnsTest {

    @Autowired
    private SysOperationLogRepository repository;

    @Autowired
    private EntityManager entityManager;

    /** 既有链尾（共享 H2 中其他测试真实提交的链行），本类全部 seq 相对其偏移。 */
    private long base;

    @org.junit.jupiter.api.BeforeEach
    void resolveChainTail() {
        base = repository.findTopBySeqIsNotNullOrderBySeqDesc()
                .map(SysOperationLog::getSeq).orElse(0L);
    }

    private static SysOperationLog newRow(final String logId, final Long seq) {
        final SysOperationLog e = new SysOperationLog();
        e.setLogId(logId);
        e.setModule("日志管理");
        e.setOperation(OperationType.QUERY);
        e.setMethod("GET");
        e.setRequestUrl("/api/v1/sys/logs");
        e.setResponseStatus(200);
        e.setIpAddress("127.0.0.1");
        e.setDurationMs(5L);
        e.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        e.setSeq(seq);
        if (seq != null) {
            e.setPrevHash("0".repeat(64));
            e.setHash("a".repeat(64));
            e.setSignature("QUJDRA==");
            e.setSignKeyId("sm2-audit-v1");
            e.setTraceId("20260612000000-000001");
        }
        return e;
    }

    @Test
    void integrityColumns_roundtripThroughDatabase() {
        final SysOperationLog row = newRow("t4log000000000000000000000000001", base + 1);
        row.setHash("b".repeat(64));
        repository.saveAndFlush(row);
        entityManager.clear();
        final SysOperationLog read = repository.findById(row.getLogId()).orElseThrow();
        assertThat(read.getSeq()).isEqualTo(base + 1);
        assertThat(read.getPrevHash()).isEqualTo("0".repeat(64));
        assertThat(read.getHash()).isEqualTo("b".repeat(64));
        assertThat(read.getSignature()).isEqualTo("QUJDRA==");
        assertThat(read.getSignKeyId()).isEqualTo("sm2-audit-v1");
        assertThat(read.getTraceId()).isEqualTo("20260612000000-000001");
    }

    @Test
    void findTopBySeq_returnsChainTail() {
        repository.saveAndFlush(newRow("t4log000000000000000000000000011", base + 1));
        repository.saveAndFlush(newRow("t4log000000000000000000000000012", base + 2));
        repository.saveAndFlush(newRow("t4log000000000000000000000000013", base + 3));
        assertThat(repository.findTopBySeqIsNotNullOrderBySeqDesc())
                .isPresent().get()
                .extracting(SysOperationLog::getSeq).isEqualTo(base + 3);
    }

    @Test
    void duplicateSeq_violatesUniqueConstraint() {
        repository.saveAndFlush(newRow("t4log000000000000000000000000021", base + 7));
        assertThatThrownBy(() -> repository.saveAndFlush(
                newRow("t4log000000000000000000000000022", base + 7)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void legacyShapeRow_allIntegrityColumnsNull_stillPersistsAndReads() {
        final SysOperationLog legacy = newRow("t4log000000000000000000000000031", null);
        repository.saveAndFlush(legacy);
        entityManager.clear();
        final SysOperationLog read = repository.findById(legacy.getLogId()).orElseThrow();
        assertThat(read.getSeq()).isNull();
        assertThat(read.getHash()).isNull();
        assertThat(read.getTraceId()).isNull();
    }

    @Test
    void findBySeqAscending_pagesChainRowsOnly() {
        repository.saveAndFlush(newRow("t4log000000000000000000000000041", null));
        repository.saveAndFlush(newRow("t4log000000000000000000000000042", base + 1));
        repository.saveAndFlush(newRow("t4log000000000000000000000000043", base + 2));
        repository.saveAndFlush(newRow("t4log000000000000000000000000044", base + 3));
        // 相对断言：本用例 3 链行升序在场且 null-seq 行被过滤（共享 H2 既有链行允许共存）
        final java.util.List<Long> seqsAfterBase = repository
                .findBySeqIsNotNullOrderBySeqAsc(org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .map(SysOperationLog::getSeq)
                .filter(q -> q > base)
                .toList();
        assertThat(seqsAfterBase).containsExactly(base + 1, base + 2, base + 3);
        // 分页形态：page size 受 Pageable 控制
        final Page<SysOperationLog> page = repository
                .findBySeqIsNotNullOrderBySeqAsc(PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }
}
