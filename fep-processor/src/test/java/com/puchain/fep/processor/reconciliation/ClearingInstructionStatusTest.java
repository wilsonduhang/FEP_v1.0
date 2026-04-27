package com.puchain.fep.processor.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enum value + alignment tests for {@link ClearingInstructionStatus}.
 *
 * <p>Guards against drift between the enum and V18 SQL constraint
 * {@code chk_clearing_status}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("ClearingInstructionStatus: enum values + V18 SQL alignment")
class ClearingInstructionStatusTest {

    @Test
    void enumShouldHaveExactlyFourValuesAlignedWithV18Sql() {
        assertThat(ClearingInstructionStatus.values())
                .containsExactly(
                        ClearingInstructionStatus.PENDING,
                        ClearingInstructionStatus.PROCESSING,
                        ClearingInstructionStatus.SUCCESS,
                        ClearingInstructionStatus.FAILED);
    }

    @Test
    void valueOf_shouldRecognizeEachV18CheckLiteral() {
        assertThat(ClearingInstructionStatus.valueOf("PENDING"))
                .isEqualTo(ClearingInstructionStatus.PENDING);
        assertThat(ClearingInstructionStatus.valueOf("PROCESSING"))
                .isEqualTo(ClearingInstructionStatus.PROCESSING);
        assertThat(ClearingInstructionStatus.valueOf("SUCCESS"))
                .isEqualTo(ClearingInstructionStatus.SUCCESS);
        assertThat(ClearingInstructionStatus.valueOf("FAILED"))
                .isEqualTo(ClearingInstructionStatus.FAILED);
    }

    @Test
    void isTerminal_shouldReturnTrueOnlyForSuccessAndFailed() {
        assertThat(ClearingInstructionStatus.PENDING.isTerminal()).isFalse();
        assertThat(ClearingInstructionStatus.PROCESSING.isTerminal()).isFalse();
        assertThat(ClearingInstructionStatus.SUCCESS.isTerminal()).isTrue();
        assertThat(ClearingInstructionStatus.FAILED.isTerminal()).isTrue();
    }
}
