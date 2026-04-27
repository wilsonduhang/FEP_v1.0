package com.puchain.fep.processor.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enum value + alignment tests for {@link ReconciliationStatus}.
 *
 * <p>Guards against drift between the enum and V18 SQL constraint
 * {@code chk_recon_status}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("ReconciliationStatus: enum values + V18 SQL alignment")
class ReconciliationStatusTest {

    @Test
    void enumShouldHaveExactlyFourValuesAlignedWithV18Sql() {
        assertThat(ReconciliationStatus.values())
                .containsExactly(
                        ReconciliationStatus.PENDING,
                        ReconciliationStatus.IN_PROGRESS,
                        ReconciliationStatus.COMPLETED,
                        ReconciliationStatus.DISCREPANCY);
    }

    @Test
    void valueOf_shouldRecognizeEachV18CheckLiteral() {
        assertThat(ReconciliationStatus.valueOf("PENDING"))
                .isEqualTo(ReconciliationStatus.PENDING);
        assertThat(ReconciliationStatus.valueOf("IN_PROGRESS"))
                .isEqualTo(ReconciliationStatus.IN_PROGRESS);
        assertThat(ReconciliationStatus.valueOf("COMPLETED"))
                .isEqualTo(ReconciliationStatus.COMPLETED);
        assertThat(ReconciliationStatus.valueOf("DISCREPANCY"))
                .isEqualTo(ReconciliationStatus.DISCREPANCY);
    }

    @Test
    void isTerminal_shouldReturnTrueOnlyForCompletedAndDiscrepancy() {
        assertThat(ReconciliationStatus.PENDING.isTerminal()).isFalse();
        assertThat(ReconciliationStatus.IN_PROGRESS.isTerminal()).isFalse();
        assertThat(ReconciliationStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(ReconciliationStatus.DISCREPANCY.isTerminal()).isTrue();
    }
}
