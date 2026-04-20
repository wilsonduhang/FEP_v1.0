package com.puchain.fep.web.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for {@link E2eSeedRunner}.
 *
 * <p>Covers the four branch scenarios the Playwright E2E gap fix must
 * guarantee: empty table insert, full-table skip, partial presence, and
 * JDBC exception propagation.</p>
 *
 * @since P7.2b
 */
@ExtendWith(MockitoExtension.class)
class E2eSeedRunnerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private E2eSeedRunner runner;

    @Test
    void run_shouldInsertAllRows_whenTablesEmpty() throws Exception {
        // Runner seeds 2 enterprises + 2 TLQ nodes = 4 INSERT calls when
        // both tables are empty.
        doReturn(0).when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), any(Object[].class));
        doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

        runner.run();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(4)).update(sql.capture(), any(Object[].class));
        assertThat(sql.getAllValues().get(0))
                .contains("INSERT INTO t_sys_enterprise")
                .contains("enterprise_id")
                .contains("enterprise_name")
                .contains("usci")
                .contains("audit_status");
        // TLQ seed INSERTs follow the enterprise ones.
        assertThat(sql.getAllValues().get(2))
                .contains("INSERT INTO t_tlq_node")
                .contains("node_id")
                .contains("node_name")
                .contains("node_role")
                .contains("host_ip")
                .contains("port");
    }

    @Test
    void run_shouldSkipAllRows_whenAlreadyPresent() throws Exception {
        doReturn(1).when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), any(Object[].class));

        runner.run();

        verify(jdbcTemplate, times(0)).update(anyString(), any(Object[].class));
    }

    @Test
    void run_shouldInsertOne_whenOneEnterpriseAlreadyPresent() throws Exception {
        // 4 SELECT COUNTs total (2 enterprise + 2 TLQ node). First enterprise
        // is present (1); remaining 3 rows absent (0) → 3 INSERTs.
        doReturn(1, 0, 0, 0).when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), any(Object[].class));
        doReturn(1).when(jdbcTemplate).update(anyString(), any(Object[].class));

        runner.run();

        verify(jdbcTemplate, times(3)).update(anyString(), any(Object[].class));
    }

    @Test
    void run_shouldPropagateJdbcException() {
        doThrow(new RuntimeException("connection lost"))
                .when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), any(Object[].class));

        assertThatThrownBy(runner::run).hasMessage("connection lost");
    }

    @Test
    void run_shouldTreatDuplicateKeyAsSkipped() throws Exception {
        // SELECT reports 0 (row not yet present), but parallel startup inserts
        // the same UNIQUE key row between our SELECT and INSERT on both tables.
        // Enterprise.usci and TlqNode.node_name are UNIQUE — DuplicateKeyException
        // should be counted as skipped, not re-thrown (would fail Spring boot
        // startup under dev-e2e profile).
        doReturn(0).when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), any(Object[].class));
        doThrow(new DuplicateKeyException("unique dup"))
                .when(jdbcTemplate).update(anyString(), any(Object[].class));

        // Should not throw — exception is swallowed and counted as skipped.
        runner.run();

        // 4 INSERT attempts: 2 enterprise + 2 TLQ node.
        verify(jdbcTemplate, times(4)).update(anyString(), any(Object[].class));
    }
}
