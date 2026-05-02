package com.puchain.fep.web.collector;

import com.puchain.fep.collector.support.JdbcWatermarkStore;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * {@link JdbcWatermarkStore} behaviour test (P4 T8).
 *
 * <p>Wires the production class against the H2 schema applied by Flyway V23.
 * The class is annotated {@code @Profile("!test")} but {@code @SpringBootTest}
 * does not activate {@code test} unless explicitly set, so the bean IS available
 * for autowiring here (matching the dev/prod activation profile).</p>
 *
 * <p>Coverage:
 * <ul>
 *   <li>{@code get} on unknown adapterId returns {@link Optional#empty}</li>
 *   <li>{@code put} then {@code get} round-trips the same value</li>
 *   <li>{@code put} overwrites a previous watermark (UPSERT semantics)</li>
 *   <li>{@code put} on different adapterIds keeps independent rows</li>
 *   <li>Mocked JdbcTemplate failure rewraps to {@code COLLECT_PERSIST_FAILURE}</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("JdbcWatermarkStore: get + UPSERT put + per-adapter isolation + DAE rewrap")
class JdbcWatermarkStoreTest {

    @Autowired
    private WatermarkStore store;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void purgeBefore() {
        jdbcTemplate.update("DELETE FROM collection_record_offset");
    }

    @AfterEach
    void purgeAfter() {
        jdbcTemplate.update("DELETE FROM collection_record_offset");
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
        final Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM collection_record_offset WHERE adapter_id = ?",
                Integer.class, "ADP_T8_W2");
        assertThat(rowCount).isEqualTo(1);
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
    void putWhenJdbcTemplateThrowsShouldRewrapToCollectPersistFailure() {
        // Use doThrow on the vararg overload to avoid Mockito matcher ambiguity
        // for varargs (when(...).thenThrow does not always bind the varargs overload
        // correctly; doThrow is unambiguous).
        final JdbcTemplate mockTpl = mock(JdbcTemplate.class);
        doThrow(new DataAccessResourceFailureException("simulated outage"))
                .when(mockTpl).update(anyString(), any(Object.class), any(Object.class), any(Object.class));

        final JdbcWatermarkStore mockStore = new JdbcWatermarkStore(mockTpl);
        assertThatThrownBy(() -> mockStore.put("ADP_BAD", "wm"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(ex -> ((FepBusinessException) ex).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_PERSIST_FAILURE);
    }

    @Test
    void getWhenJdbcTemplateThrowsShouldRewrapToCollectPersistFailure() {
        final JdbcTemplate mockTpl = mock(JdbcTemplate.class);
        // queryForObject(sql, Class<T>, Object...) overload — pin args by anyString to
        // bind to the (String, Class, Object...) signature unambiguously.
        doThrow(new DataAccessResourceFailureException("simulated outage"))
                .when(mockTpl).queryForObject(anyString(), eq(String.class), any(Object.class));

        final JdbcWatermarkStore mockStore = new JdbcWatermarkStore(mockTpl);
        assertThatThrownBy(() -> mockStore.get("ADP_BAD"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(ex -> ((FepBusinessException) ex).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_PERSIST_FAILURE);
    }
}
