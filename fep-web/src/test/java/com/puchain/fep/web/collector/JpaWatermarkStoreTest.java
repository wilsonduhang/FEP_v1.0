package com.puchain.fep.web.collector;

import com.puchain.fep.collector.support.WatermarkStore;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link JpaWatermarkStore} behaviour test (P4 T8-fix).
 *
 * <p>Wires the production class against the H2 schema applied by Flyway V23,
 * mirroring the T7a precedent
 * ({@link com.puchain.fep.web.outbound.JpaOutboundMessageEnqueueServiceTest}).
 * Exercises {@code @Profile("!test")} activation by relying on
 * {@code @SpringBootTest}'s default profile (no {@code test} activation), so
 * the JPA-backed bean is the wired {@link WatermarkStore} implementation.</p>
 *
 * <p>Coverage:
 * <ul>
 *   <li>{@code get} on unknown adapterId returns {@link Optional#empty}</li>
 *   <li>{@code put} then {@code get} round-trips the same value</li>
 *   <li>{@code put} overwrites a previous watermark (UPSERT semantics)</li>
 *   <li>{@code put} on different adapterIds keeps independent rows</li>
 *   <li>128-character VARCHAR boundary value round-trips intact</li>
 *   <li>Mocked repository failure rewraps to {@code COLLECT_PERSIST_FAILURE}</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("JpaWatermarkStore: get + UPSERT put + per-adapter isolation + DAE rewrap")
class JpaWatermarkStoreTest {

    @Autowired
    private WatermarkStore store;

    @Autowired
    private CollectionRecordOffsetRepository repository;

    @BeforeEach
    void purgeBefore() {
        repository.deleteAll();
    }

    @AfterEach
    void purgeAfter() {
        repository.deleteAll();
    }

    @Test
    void getUnknownAdapterIdShouldReturnEmpty() {
        assertThat(store.get("ADP_NEVER_SEEN_BEFORE"))
                .as("first-run semantics: unknown adapterId returns Optional.empty")
                .isEmpty();
    }

    @Test
    void putThenGetShouldReturnSameValue() {
        store.put("ADP_T8_W1", "2026-04-30T10:00:00Z");

        assertThat(store.get("ADP_T8_W1")).contains("2026-04-30T10:00:00Z");
    }

    @Test
    void putShouldOverwritePreviousWatermark() {
        store.put("ADP_T8_W2", "2026-04-29T00:00:00Z");
        store.put("ADP_T8_W2", "2026-04-30T00:00:00Z");

        assertThat(store.get("ADP_T8_W2"))
                .as("UPSERT must replace the prior value (watermark advances)")
                .contains("2026-04-30T00:00:00Z");
        // Exactly one row for the adapterId — UPSERT, not INSERT
        assertThat(repository.findAll())
                .filteredOn(e -> "ADP_T8_W2".equals(e.getAdapterId()))
                .hasSize(1);
    }

    @Test
    void putOnDifferentAdaptersShouldKeepIndependentRows() {
        store.put("ADP_T8_W3a", "wm-a");
        store.put("ADP_T8_W3b", "wm-b");

        assertThat(store.get("ADP_T8_W3a")).contains("wm-a");
        assertThat(store.get("ADP_T8_W3b")).contains("wm-b");
    }

    @Test
    void putWith128CharBoundaryWatermarkShouldRoundTrip() {
        // Schema: VARCHAR(128). Verify the exact column boundary.
        final String boundary = "W".repeat(128);
        store.put("ADP_T8_BOUND", boundary);
        assertThat(store.get("ADP_T8_BOUND")).contains(boundary);
    }

    @Test
    void putWithBlankWatermarkShouldThrow_T10SimplifyQ3() {
        // T10 Simplify Q-3 fix: blank watermark would degrade to "WHERE cursor > ''"
        // on the next collect → silent re-fetch of all source rows. Reject at
        // the persistence boundary; nothing must be written.
        assertThatThrownBy(() -> store.put("ADP_T10_Q3", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> store.put("ADP_T10_Q3", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
        assertThat(store.get("ADP_T10_Q3"))
                .as("blank watermark must NOT be persisted")
                .isEmpty();
    }

    @Test
    void putWhenRepositoryThrowsShouldRewrapToCollectPersistFailure() {
        // Mock-driven path: prove the catch block surfaces the canonical
        // FepErrorCode.COLLECT_PERSIST_FAILURE without leaking the JPA exception.
        // No vararg matcher ambiguity here — repository.save takes a single argument.
        final CollectionRecordOffsetRepository mockRepo = mock(CollectionRecordOffsetRepository.class);
        when(mockRepo.save(any(CollectionRecordOffsetEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("simulated outage"));

        final JpaWatermarkStore mockStore = new JpaWatermarkStore(mockRepo);
        assertThatThrownBy(() -> mockStore.put("ADP_BAD", "wm"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(ex -> ((FepBusinessException) ex).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_PERSIST_FAILURE);
    }

    @Test
    void getWhenRepositoryThrowsShouldRewrapToCollectPersistFailure() {
        final CollectionRecordOffsetRepository mockRepo = mock(CollectionRecordOffsetRepository.class);
        when(mockRepo.findById(any(String.class)))
                .thenThrow(new DataAccessResourceFailureException("simulated outage"));

        final JpaWatermarkStore mockStore = new JpaWatermarkStore(mockRepo);
        assertThatThrownBy(() -> mockStore.get("ADP_BAD"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(ex -> ((FepBusinessException) ex).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_PERSIST_FAILURE);
    }
}
