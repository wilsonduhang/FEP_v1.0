package com.puchain.fep.web.collector;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema + Repository CRUD + Specification verification for V23
 * {@code collection_run} (P4 T8).
 *
 * <p>Uses {@code @SpringBootTest} (not {@code @DataJpaTest}) per the T7a
 * precedent ({@link com.puchain.fep.web.outbound.JpaOutboundMessageEnqueueServiceTest})
 * because the H2 {@code MODE=MySQL} schema requires the full Flyway-applied
 * application context.</p>
 *
 * <p>Coverage:
 * <ul>
 *   <li>V23 schema indexes exist (queried via H2 INFORMATION_SCHEMA.INDEXES)</li>
 *   <li>Round-trip persistence preserves all 12 columns</li>
 *   <li>Specification filter on {@code adapterId + status + started_at} range</li>
 *   <li>Pageable sort by {@code started_at DESC} returns rows in expected order</li>
 *   <li>Boundary: error_message at-cap (1024 chars) persists intact</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("V23 collection_run schema + Repository CRUD + Specification + indexes")
class CollectionRunRepositoryTest {

    @Autowired
    private CollectionRunRepository repository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void purgeBefore() {
        repository.deleteAll();
    }

    @AfterEach
    void purgeAfter() {
        repository.deleteAll();
    }

    /**
     * Asserts H2's rendering of V23 indexes (this query targets H2's
     * {@code INFORMATION_SCHEMA.INDEXES} view, not standard SQL). Production
     * MySQL 8 index creation is verified via Flyway-to-MySQL CI in nightly
     * runs (when available); the V23 SQL syntax for the {@code CREATE INDEX}
     * statements is portable across H2 + MySQL, so a green H2 assertion is
     * an acceptable proxy until the MySQL CI lane lands (T8-fix MEDIUM #4).
     */
    @Test
    void v23SchemaIndexesShouldExistOnCollectionRun() {
        // H2 INFORMATION_SCHEMA.INDEXES query: lower-case because DATABASE_TO_LOWER=TRUE.
        @SuppressWarnings("unchecked")
        final List<String> indexNames = entityManager.createNativeQuery(
                "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES "
                        + "WHERE TABLE_NAME = 'collection_run'")
                .getResultList();

        assertThat(indexNames)
                .as("V23 must declare two indexes on collection_run")
                .contains("idx_collection_run_adapter_started", "idx_collection_run_status");
    }

    @Test
    void roundTripPersistShouldPreserveAllColumns() {
        final Instant startedAt = Instant.parse("2026-04-30T10:00:00Z");
        final Instant completedAt = startedAt.plus(5, ChronoUnit.SECONDS);

        final CollectionRunEntity entity = newRow(
                "RUN000000000000000000000000000A1",
                "ADP_TEST_001",
                "SUCCESS",
                startedAt, completedAt,
                10, 9, 9, 1,
                "first failure: missing field",
                "MANUAL");
        repository.save(entity);

        final CollectionRunEntity loaded = repository.findById(entity.getRunId()).orElseThrow();
        assertThat(loaded.getRunId()).isEqualTo("RUN000000000000000000000000000A1");
        assertThat(loaded.getAdapterId()).isEqualTo("ADP_TEST_001");
        assertThat(loaded.getStatus()).isEqualTo("SUCCESS");
        assertThat(loaded.getStartedAt()).isEqualTo(startedAt);
        assertThat(loaded.getCompletedAt()).isEqualTo(completedAt);
        assertThat(loaded.getCollectedCount()).isEqualTo(10);
        assertThat(loaded.getAssembledCount()).isEqualTo(9);
        assertThat(loaded.getSubmittedCount()).isEqualTo(9);
        assertThat(loaded.getErrorCount()).isEqualTo(1);
        assertThat(loaded.getErrorMessage()).isEqualTo("first failure: missing field");
        assertThat(loaded.getTriggerSource()).isEqualTo("MANUAL");
        assertThat(loaded.getCreatedAt()).isEqualTo(startedAt);
    }

    @Test
    void specificationShouldFilterByAdapterIdStatusAndStartedAtRange() {
        // 4 rows: 3 for ADP_A (varied status + started_at), 1 for ADP_B
        final Instant base = Instant.parse("2026-04-29T00:00:00Z");
        repository.save(newRow("R00000000000000000000000000000A1", "ADP_A", "SUCCESS",
                base, base.plusSeconds(1), 5, 5, 5, 0, null, "SCHEDULED"));
        repository.save(newRow("R00000000000000000000000000000A2", "ADP_A", "FAILED",
                base.plusSeconds(60), base.plusSeconds(61), 5, 0, 0, 5, "boom", "MANUAL"));
        repository.save(newRow("R00000000000000000000000000000A3", "ADP_A", "SUCCESS",
                base.plusSeconds(120), base.plusSeconds(121), 5, 5, 5, 0, null, "SCHEDULED"));
        repository.save(newRow("R00000000000000000000000000000B1", "ADP_B", "SUCCESS",
                base.plusSeconds(60), base.plusSeconds(61), 5, 5, 5, 0, null, "SCHEDULED"));

        final Specification<CollectionRunEntity> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("adapterId"), "ADP_A"),
                cb.equal(root.get("status"), "SUCCESS"),
                cb.greaterThanOrEqualTo(root.<Instant>get("startedAt"), base),
                cb.lessThanOrEqualTo(root.<Instant>get("startedAt"), base.plusSeconds(120)));

        final List<CollectionRunEntity> rows = repository.findAll(spec,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startedAt"))).getContent();

        assertThat(rows)
                .as("Specification + DESC sort must return ADP_A SUCCESS rows newest-first")
                .extracting(CollectionRunEntity::getRunId)
                .containsExactly(
                        "R00000000000000000000000000000A3",
                        "R00000000000000000000000000000A1");
    }

    /**
     * Boundary coverage for the {@code error_message} TEXT column —
     * scheduler caps at 1024 chars (CollectorScheduler.ERROR_MESSAGE_MAX_LENGTH);
     * the schema must accept that exact length intact.
     */
    @Test
    void errorMessageAtSchedulerCapShouldPersistIntact() {
        final String capped = "X".repeat(1024);
        final Instant now = Instant.parse("2026-04-30T11:00:00Z");
        final CollectionRunEntity entity = newRow(
                "R00000000000000000000000000CAP01", "ADP_LIMITS", "FAILED",
                now, now.plusSeconds(1), 1, 0, 0, 1, capped, "MANUAL");
        repository.save(entity);

        final String loaded = repository.findById(entity.getRunId()).orElseThrow().getErrorMessage();
        assertThat(loaded).hasSize(1024);
        assertThat(loaded).isEqualTo(capped);
    }

    @Test
    void runningRowWithNullCompletedAtShouldRoundTrip() {
        // start() persists a row before complete() fires; completed_at must be nullable.
        final Instant startedAt = Instant.parse("2026-04-30T12:00:00Z");
        final CollectionRunEntity entity = newRow(
                "R00000000000000000000000000RUN01", "ADP_RUN", "RUNNING",
                startedAt, null, 0, 0, 0, 0, null, "SCHEDULED");
        repository.save(entity);

        final CollectionRunEntity loaded = repository.findById(entity.getRunId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo("RUNNING");
        assertThat(loaded.getCompletedAt()).isNull();
        assertThat(loaded.getErrorMessage()).isNull();
    }

    private static CollectionRunEntity newRow(final String runId, final String adapterId,
                                              final String status,
                                              final Instant startedAt, final Instant completedAt,
                                              final int collected, final int assembled,
                                              final int submitted, final int errors,
                                              final String errorMessage,
                                              final String triggerSource) {
        final CollectionRunEntity e = new CollectionRunEntity();
        e.setRunId(runId);
        e.setAdapterId(adapterId);
        e.setStatus(status);
        e.setStartedAt(startedAt);
        e.setCompletedAt(completedAt);
        e.setCollectedCount(collected);
        e.setAssembledCount(assembled);
        e.setSubmittedCount(submitted);
        e.setErrorCount(errors);
        e.setErrorMessage(errorMessage);
        e.setTriggerSource(triggerSource);
        e.setCreatedAt(startedAt);
        return e;
    }
}
