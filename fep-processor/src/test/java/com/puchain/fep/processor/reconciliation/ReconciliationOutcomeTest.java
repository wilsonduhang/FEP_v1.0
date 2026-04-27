package com.puchain.fep.processor.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Compact constructor invariant tests for {@link ReconciliationOutcome}.
 *
 * <p>Validates the four invariants:
 * status non-null, all counts non-negative, and the single-direction
 * COMPLETED&nbsp;⇒&nbsp;discrepancyCount==0 guard.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("ReconciliationOutcome: compact constructor invariants")
class ReconciliationOutcomeTest {

    @Test
    void completedWithZeroDiscrepancy_shouldConstructSuccessfully() {
        final ReconciliationOutcome r =
                new ReconciliationOutcome(10, 10, ReconciliationStatus.COMPLETED, 0);

        assertThat(r.declaredCount()).isEqualTo(10);
        assertThat(r.actualSize()).isEqualTo(10);
        assertThat(r.status()).isEqualTo(ReconciliationStatus.COMPLETED);
        assertThat(r.discrepancyCount()).isZero();
    }

    @Test
    void discrepancyWithNonZeroCount_shouldConstructSuccessfully() {
        final ReconciliationOutcome r =
                new ReconciliationOutcome(10, 8, ReconciliationStatus.DISCREPANCY, 2);

        assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
        assertThat(r.discrepancyCount()).isEqualTo(2);
    }

    @Test
    void discrepancyWithZeroCount_shouldConstructSuccessfully_forEmptyBusinessRuleCase() {
        // v1b: empty qsInfoList → DISCREPANCY+0 is a legal "未提交清算指令" semantic.
        final ReconciliationOutcome r =
                new ReconciliationOutcome(0, 0, ReconciliationStatus.DISCREPANCY, 0);

        assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
        assertThat(r.discrepancyCount()).isZero();
    }

    @Test
    void nullStatus_shouldThrowNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ReconciliationOutcome(0, 0, null, 0))
                .withMessageContaining("status");
    }

    @Test
    void negativeDeclaredCount_shouldThrowIae() {
        assertThatThrownBy(() ->
                new ReconciliationOutcome(-1, 0, ReconciliationStatus.PENDING, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("declaredCount");
    }

    @Test
    void negativeActualSize_shouldThrowIae() {
        assertThatThrownBy(() ->
                new ReconciliationOutcome(0, -1, ReconciliationStatus.PENDING, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actualSize");
    }

    @Test
    void negativeDiscrepancyCount_shouldThrowIae() {
        assertThatThrownBy(() ->
                new ReconciliationOutcome(0, 0, ReconciliationStatus.PENDING, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discrepancyCount");
    }

    @Test
    void completedWithNonZeroDiscrepancy_shouldThrowIse() {
        assertThatThrownBy(() ->
                new ReconciliationOutcome(10, 10, ReconciliationStatus.COMPLETED, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED requires discrepancyCount=0");
    }
}
