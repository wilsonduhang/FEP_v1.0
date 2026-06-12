package com.puchain.fep.web.sysmgmt.log.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * checkpoint 锚 entity/repository 持久化语义（EFF-S5-1 T1）。
 *
 * <p>单行锚：PK 固定 {@link AuditChainCheckpoint#SINGLETON_ID}，二次 save 为
 * 覆盖（JPA merge）非第二行。@Transactional 末尾回滚，不污染 shared dev H2。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AuditChainCheckpointTest {

    @Autowired
    private AuditChainCheckpointRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findById_onEmptyTable_returnsEmpty() {
        assertThat(repository.findById(AuditChainCheckpoint.SINGLETON_ID)).isEmpty();
    }

    @Test
    void saveAndFindById_roundTripsAllColumns() {
        final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        final AuditChainCheckpoint cp = new AuditChainCheckpoint();
        cp.setVerifiedUntilSeq(42L);
        cp.setAnchorHash("a".repeat(64));
        cp.setCheckpointSignature("c2lnbmF0dXJl");
        cp.setSignKeyId("sm2-audit-v1");
        cp.setVerifiedAt(now);
        repository.saveAndFlush(cp);

        final AuditChainCheckpoint read =
                repository.findById(AuditChainCheckpoint.SINGLETON_ID).orElseThrow();
        assertThat(read.getId()).isEqualTo(AuditChainCheckpoint.SINGLETON_ID);
        assertThat(read.getVerifiedUntilSeq()).isEqualTo(42L);
        assertThat(read.getAnchorHash()).isEqualTo("a".repeat(64));
        assertThat(read.getCheckpointSignature()).isEqualTo("c2lnbmF0dXJl");
        assertThat(read.getSignKeyId()).isEqualTo("sm2-audit-v1");
        assertThat(read.getVerifiedAt()).isEqualTo(now);
    }

    @Test
    void secondSaveWithSameId_overwritesSingleRow() {
        final AuditChainCheckpoint first = new AuditChainCheckpoint();
        first.setVerifiedUntilSeq(1L);
        first.setAnchorHash("a".repeat(64));
        first.setCheckpointSignature("c2ln");
        first.setSignKeyId("sm2-audit-v1");
        first.setVerifiedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        repository.saveAndFlush(first);

        final AuditChainCheckpoint second = new AuditChainCheckpoint();
        second.setVerifiedUntilSeq(7L);
        second.setAnchorHash("b".repeat(64));
        second.setCheckpointSignature("c2lnMg==");
        second.setSignKeyId("sm2-audit-v2");
        second.setVerifiedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        repository.saveAndFlush(second);

        assertThat(repository.count()).isEqualTo(1L);
        final AuditChainCheckpoint read =
                repository.findById(AuditChainCheckpoint.SINGLETON_ID).orElseThrow();
        assertThat(read.getVerifiedUntilSeq()).isEqualTo(7L);
        assertThat(read.getAnchorHash()).isEqualTo("b".repeat(64));
        assertThat(read.getSignKeyId()).isEqualTo("sm2-audit-v2");
    }
}
