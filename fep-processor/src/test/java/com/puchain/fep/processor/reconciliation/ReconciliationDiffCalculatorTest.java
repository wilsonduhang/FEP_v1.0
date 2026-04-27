package com.puchain.fep.processor.reconciliation;

import com.puchain.fep.processor.body.supplychain.QsInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReconciliationDiffCalculator}.
 *
 * <p>Coverage: count-diff (6 cases) + business-rule (6 cases) + boundaries (2 cases) = 14 tests.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("ReconciliationDiffCalculator: count diff + 3115 amt business rule")
class ReconciliationDiffCalculatorTest {

    private ReconciliationDiffCalculator calc;

    @BeforeEach
    void setUp() {
        calc = new ReconciliationDiffCalculator();
    }

    @Nested
    @DisplayName("calculateCountDiff: 6 cases")
    class CountDiffTests {

        @Test
        void equal_shouldBeCompleted() {
            final ReconciliationOutcome r = calc.calculateCountDiff(10, 10);

            assertThat(r.declaredCount()).isEqualTo(10);
            assertThat(r.actualSize()).isEqualTo(10);
            assertThat(r.status()).isEqualTo(ReconciliationStatus.COMPLETED);
            assertThat(r.discrepancyCount()).isZero();
        }

        @Test
        void underReport_shouldBeDiscrepancy() {
            final ReconciliationOutcome r = calc.calculateCountDiff(10, 8);

            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isEqualTo(2);
        }

        @Test
        void overReport_shouldBeDiscrepancy() {
            final ReconciliationOutcome r = calc.calculateCountDiff(8, 10);

            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isEqualTo(2);
        }

        @Test
        void zeroBoth_shouldBeCompleted() {
            final ReconciliationOutcome r = calc.calculateCountDiff(0, 0);

            assertThat(r.status()).isEqualTo(ReconciliationStatus.COMPLETED);
            assertThat(r.discrepancyCount()).isZero();
        }

        @Test
        void negativeDeclared_shouldThrowIae() {
            assertThatThrownBy(() -> calc.calculateCountDiff(-1, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("declared");
        }

        @Test
        void negativeActual_shouldThrowIae() {
            assertThatThrownBy(() -> calc.calculateCountDiff(0, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("actual");
        }
    }

    @Nested
    @DisplayName("validateBusinessRule: 6 cases")
    class BusinessRuleTests {

        @Test
        void nullList_shouldBeEmptyDiscrepancy() {
            final ReconciliationOutcome r = calc.validateBusinessRule(null);

            assertThat(r.declaredCount()).isZero();
            assertThat(r.actualSize()).isZero();
            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isZero();
        }

        @Test
        void emptyList_shouldBeEmptyDiscrepancy() {
            final ReconciliationOutcome r = calc.validateBusinessRule(Collections.emptyList());

            assertThat(r.declaredCount()).isZero();
            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isZero();
        }

        @Test
        void allValidAmounts_shouldBeCompleted() {
            final List<QsInfo> list = Arrays.asList(
                    qsWithAmt("100.00"),
                    qsWithAmt("250.50"),
                    qsWithAmt("0.01"));

            final ReconciliationOutcome r = calc.validateBusinessRule(list);

            assertThat(r.declaredCount()).isEqualTo(3);
            assertThat(r.actualSize()).isEqualTo(3);
            assertThat(r.status()).isEqualTo(ReconciliationStatus.COMPLETED);
            assertThat(r.discrepancyCount()).isZero();
        }

        @Test
        void oneInvalidNullAmt_shouldBeDiscrepancy() {
            final List<QsInfo> list = Arrays.asList(
                    qsWithAmt("100.00"),
                    qsWithAmt(null),
                    qsWithAmt("250.50"));

            final ReconciliationOutcome r = calc.validateBusinessRule(list);

            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isEqualTo(1);
            assertThat(r.actualSize()).isEqualTo(3);
        }

        @Test
        void allInvalidZero_shouldBeDiscrepancy() {
            final List<QsInfo> list = Arrays.asList(
                    qsWithAmt("0.00"),
                    qsWithAmt("0.00"),
                    qsWithAmt("0.00"));

            final ReconciliationOutcome r = calc.validateBusinessRule(list);

            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isEqualTo(3);
        }

        @Test
        void mixedNegativeAndValid_shouldBeDiscrepancy() {
            final List<QsInfo> list = Arrays.asList(
                    qsWithAmt("100.00"),
                    qsWithAmt("-50.00"),
                    qsWithAmt("200.00"));

            final ReconciliationOutcome r = calc.validateBusinessRule(list);

            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Boundaries: 2 cases")
    class BoundaryTests {

        @Test
        void zeroAmtVariousFormats_allInvalid() {
            final List<QsInfo> list = Arrays.asList(
                    qsWithAmt("0"),
                    qsWithAmt("0.0"),
                    qsWithAmt("0.00"),
                    qsWithAmt("0.0000"));

            final ReconciliationOutcome r = calc.validateBusinessRule(list);

            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isEqualTo(4);
            assertThat(r.actualSize()).isEqualTo(4);
        }

        @Test
        void blankAndNonNumericAmt_allInvalid() {
            final List<QsInfo> list = new ArrayList<>();
            list.add(qsWithAmt(""));
            list.add(qsWithAmt("   "));
            list.add(qsWithAmt("abc"));
            list.add(qsWithAmt(null));

            final ReconciliationOutcome r = calc.validateBusinessRule(list);

            assertThat(r.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
            assertThat(r.discrepancyCount()).isEqualTo(4);
        }
    }

    private static QsInfo qsWithAmt(final String amt) {
        final QsInfo q = new QsInfo();
        q.setAmt(amt);
        return q;
    }
}
