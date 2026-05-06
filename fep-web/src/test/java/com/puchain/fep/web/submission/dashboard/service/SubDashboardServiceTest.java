package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.web.submission.dashboard.dto.DashboardDistributionItem;
import com.puchain.fep.web.submission.dashboard.dto.DashboardResponse;
import com.puchain.fep.web.submission.dashboard.dto.DashboardTrendResponse;
import com.puchain.fep.web.submission.datasource.repository.SubDataSourceRepository;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.submission.record.repository.SubSubmissionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SubDashboardService unit tests.
 */
@ExtendWith(MockitoExtension.class)
class SubDashboardServiceTest {

    @Mock
    private SubOutputInterfaceRepository outputInterfaceRepository;

    @Mock
    private SubDataSourceRepository dataSourceRepository;

    @Mock
    private SubSubmissionRecordRepository recordRepository;

    private SubDashboardService service;

    @BeforeEach
    void setUp() {
        service = new SubDashboardService(
                outputInterfaceRepository, dataSourceRepository, recordRepository);
    }

    @Test
    void getDashboard_shouldAggregateAllCounts() {
        // ifaceCounts[0]=total, ifaceCounts[1]=enabled — 单行聚合包装 List 返回
        when(outputInterfaceRepository.aggregateInterfaceCounts())
                .thenReturn(List.<Object[]>of(new Object[] {10L, 7L}));
        when(dataSourceRepository.count()).thenReturn(3L);
        // recordCounts[0]=total, [1]=pushed, [2]=pending
        when(recordRepository.aggregatePushStatusCounts())
                .thenReturn(List.<Object[]>of(new Object[] {500L, 350L, 150L}));

        final DashboardResponse resp = service.getDashboard();

        assertThat(resp.getTotalInterfaceCount()).isEqualTo(10);
        assertThat(resp.getEnabledInterfaceCount()).isEqualTo(7);
        assertThat(resp.getTotalDataSourceCount()).isEqualTo(3);
        assertThat(resp.getTotalRecordCount()).isEqualTo(500);
        assertThat(resp.getPushedRecordCount()).isEqualTo(350);
        assertThat(resp.getPendingRecordCount()).isEqualTo(150);
    }

    @Test
    void getTrend_7days_shouldReturn7DateElements() {
        when(recordRepository.aggregateTrendByDate(any(), any())).thenReturn(List.of());

        final DashboardTrendResponse resp = service.getTrend(7);

        assertThat(resp.getDates()).hasSize(7);
        assertThat(resp.getPushedCounts()).hasSize(7);
        assertThat(resp.getPendingCounts()).hasSize(7);
        // All zero when repo returns empty list
        assertThat(resp.getPushedCounts()).allMatch(v -> v == 0L);
        assertThat(resp.getPendingCounts()).allMatch(v -> v == 0L);
        // Ascending dates: last element = today
        assertThat(resp.getDates().get(6)).isEqualTo(LocalDate.now().toString());
        assertThat(resp.getDates().get(0))
                .isEqualTo(LocalDate.now().minusDays(6L).toString());
    }

    @Test
    void getTrend_30days_shouldReturn30DateElements() {
        when(recordRepository.aggregateTrendByDate(any(), any())).thenReturn(List.of());

        final DashboardTrendResponse resp = service.getTrend(30);

        assertThat(resp.getDates()).hasSize(30);
        assertThat(resp.getPushedCounts()).hasSize(30);
        assertThat(resp.getPendingCounts()).hasSize(30);
    }

    @Test
    void getTrend_days1_shouldThrowIAE() {
        assertThatThrownBy(() -> service.getTrend(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days must be 7 or 30");
    }

    @Test
    void getTrend_days100_shouldThrowIAE() {
        assertThatThrownBy(() -> service.getTrend(100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days must be 7 or 30");
    }

    @Test
    void getTrend_midWindowBucket_mapsCorrectly() {
        // 7-day window: index 0 = today-6, index 6 = today. Mid bucket at
        // index 3 = today-3. Pin the returned index so future refactors of the
        // fill loop (e.g., reversed ordering) regress visibly.
        final LocalDate today = LocalDate.now();
        final LocalDate mid = today.minusDays(3L);
        when(recordRepository.aggregateTrendByDate(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {mid.toString(), 9L, 4L}));

        final DashboardTrendResponse resp = service.getTrend(7);

        assertThat(resp.getDates()).hasSize(7);
        assertThat(resp.getPushedCounts().get(3)).isEqualTo(9L);
        assertThat(resp.getPendingCounts().get(3)).isEqualTo(4L);
        // Neighboring buckets stay zero.
        assertThat(resp.getPushedCounts().get(0)).isZero();
        assertThat(resp.getPushedCounts().get(6)).isZero();
    }

    @Test
    void getTrend_onlyTodayHasData_shouldFillPrecedingDaysWithZero() {
        final String today = LocalDate.now().toString();
        // Repo returns one row for today: pushed=5, pending=2
        final List<Object[]> rows = List.<Object[]>of(new Object[] {today, 5L, 2L});
        when(recordRepository.aggregateTrendByDate(any(), any())).thenReturn(rows);

        final DashboardTrendResponse resp = service.getTrend(7);

        assertThat(resp.getDates()).hasSize(7);
        // Index 0..5 are pre-today, should all be zero
        for (int i = 0; i < 6; i++) {
            assertThat(resp.getPushedCounts().get(i)).as("pushed[%d]", i).isEqualTo(0L);
            assertThat(resp.getPendingCounts().get(i)).as("pending[%d]", i).isEqualTo(0L);
        }
        // Index 6 is today, should carry the aggregated counts
        assertThat(resp.getDates().get(6)).isEqualTo(today);
        assertThat(resp.getPushedCounts().get(6)).isEqualTo(5L);
        assertThat(resp.getPendingCounts().get(6)).isEqualTo(2L);
    }

    @Test
    void getDistribution_byMessageType_shouldReturnDescendingByValue() {
        final List<Object[]> rows = List.<Object[]>of(
                new Object[] {"3101", 42L},
                new Object[] {"1101", 17L}
        );
        when(recordRepository.aggregateDistributionByMessageType(
                        any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(rows);

        final List<DashboardDistributionItem> items = service.getDistribution("messageType", null);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getName()).isEqualTo("3101");
        assertThat(items.get(0).getValue()).isEqualTo(42L);
        assertThat(items.get(1).getName()).isEqualTo("1101");
        assertThat(items.get(1).getValue()).isEqualTo(17L);
    }

    @Test
    void getDistribution_byBusinessType_shouldReturnDescendingByValue() {
        final List<Object[]> rows = List.<Object[]>of(
                new Object[] {"BIZ_A", 30L},
                new Object[] {"UNSPECIFIED", 10L}
        );
        when(recordRepository.aggregateDistributionByBusinessType(
                        any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(rows);

        final List<DashboardDistributionItem> items = service.getDistribution("businessType", null);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getName()).isEqualTo("BIZ_A");
        assertThat(items.get(0).getValue()).isEqualTo(30L);
        assertThat(items.get(1).getName()).isEqualTo("UNSPECIFIED");
        assertThat(items.get(1).getValue()).isEqualTo(10L);
    }

    @Test
    void getDistribution_invalidDim_shouldThrowIAE() {
        assertThatThrownBy(() -> service.getDistribution("foo", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dim must be messageType or businessType");
    }

    // ===== R4 Task 1: time-window boundary cases =====

    @Test
    void getDistribution_messageType_defaultDays_passes90DaysAgoToRepo() {
        when(recordRepository.aggregateDistributionByMessageType(
                        any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        service.getDistribution("messageType", null);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(recordRepository).aggregateDistributionByMessageType(
                captor.capture(), any(Pageable.class));
        LocalDateTime expected = LocalDate.now().minusDays(90L).atStartOfDay();
        assertThat(captor.getValue()).isCloseTo(expected, within(1L, ChronoUnit.SECONDS));
    }

    @Test
    void getDistribution_explicitDays30_passes30DaysAgoToRepo() {
        when(recordRepository.aggregateDistributionByBusinessType(
                        any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        service.getDistribution("businessType", 30);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(recordRepository).aggregateDistributionByBusinessType(
                captor.capture(), any(Pageable.class));
        LocalDateTime expected = LocalDate.now().minusDays(30L).atStartOfDay();
        assertThat(captor.getValue()).isCloseTo(expected, within(1L, ChronoUnit.SECONDS));
    }

    @Test
    void getDistribution_messageType_explicitDays30_passes30DaysAgoToRepo() {
        when(recordRepository.aggregateDistributionByMessageType(
                        any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        service.getDistribution("messageType", 30);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(recordRepository).aggregateDistributionByMessageType(
                captor.capture(), any(Pageable.class));
        LocalDateTime expected = LocalDate.now().minusDays(30L).atStartOfDay();
        assertThat(captor.getValue()).isCloseTo(expected, within(1L, ChronoUnit.SECONDS));
    }

    @Test
    void getDistribution_daysZero_throwsIAE() {
        assertThatThrownBy(() -> service.getDistribution("messageType", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days");
    }

    @Test
    void getDistribution_daysNegative_throwsIAE() {
        assertThatThrownBy(() -> service.getDistribution("messageType", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days");
    }

    @Test
    void getDistribution_daysOver365_throwsIAE() {
        assertThatThrownBy(() -> service.getDistribution("messageType", 366))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("days");
    }

    @Test
    void getDistribution_invalidDim_throwsIAEBeforeDaysCheck() {
        // dim validation must run before days validation: passing days=0 with
        // an invalid dim should still surface the dim error, not the days error.
        assertThatThrownBy(() -> service.getDistribution("invalid", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dim");
    }
}
