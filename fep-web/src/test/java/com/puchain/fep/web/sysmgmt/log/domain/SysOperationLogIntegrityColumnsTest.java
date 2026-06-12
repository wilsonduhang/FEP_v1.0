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
 * <p>@Transactional 末尾回滚隔离；uk_audit_seq 冲突经 saveAndFlush 强制触发。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class SysOperationLogIntegrityColumnsTest {

    @Autowired
    private SysOperationLogRepository repository;

    @Autowired
    private EntityManager entityManager;

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
        final SysOperationLog row = newRow("t4log000000000000000000000000001", 1L);
        row.setHash("b".repeat(64));
        repository.saveAndFlush(row);
        entityManager.clear();
        final SysOperationLog read = repository.findById(row.getLogId()).orElseThrow();
        assertThat(read.getSeq()).isEqualTo(1L);
        assertThat(read.getPrevHash()).isEqualTo("0".repeat(64));
        assertThat(read.getHash()).isEqualTo("b".repeat(64));
        assertThat(read.getSignature()).isEqualTo("QUJDRA==");
        assertThat(read.getSignKeyId()).isEqualTo("sm2-audit-v1");
        assertThat(read.getTraceId()).isEqualTo("20260612000000-000001");
    }

    @Test
    void findTopBySeq_returnsChainTail() {
        repository.saveAndFlush(newRow("t4log000000000000000000000000011", 1L));
        repository.saveAndFlush(newRow("t4log000000000000000000000000012", 2L));
        repository.saveAndFlush(newRow("t4log000000000000000000000000013", 3L));
        assertThat(repository.findTopBySeqIsNotNullOrderBySeqDesc())
                .isPresent().get()
                .extracting(SysOperationLog::getSeq).isEqualTo(3L);
    }

    @Test
    void duplicateSeq_violatesUniqueConstraint() {
        repository.saveAndFlush(newRow("t4log000000000000000000000000021", 7L));
        assertThatThrownBy(() -> repository.saveAndFlush(
                newRow("t4log000000000000000000000000022", 7L)))
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
        repository.saveAndFlush(newRow("t4log000000000000000000000000042", 1L));
        repository.saveAndFlush(newRow("t4log000000000000000000000000043", 2L));
        repository.saveAndFlush(newRow("t4log000000000000000000000000044", 3L));
        final Page<SysOperationLog> page = repository
                .findBySeqIsNotNullOrderBySeqAsc(PageRequest.of(0, 2));
        assertThat(page.getContent()).extracting(SysOperationLog::getSeq)
                .containsExactly(1L, 2L);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }
}
