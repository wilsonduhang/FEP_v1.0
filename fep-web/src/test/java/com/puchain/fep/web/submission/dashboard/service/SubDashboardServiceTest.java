package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.dashboard.dto.DashboardDistributionItem;
import com.puchain.fep.web.submission.dashboard.dto.DashboardResponse;
import com.puchain.fep.web.submission.dashboard.dto.DashboardTrendResponse;
import com.puchain.fep.web.submission.datasource.repository.SubDataSourceRepository;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.repository.SubSubmissionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        when(outputInterfaceRepository.count()).thenReturn(10L);
        when(outputInterfaceRepository.countByInterfaceStatus(EnableDisableStatus.ENABLED))
                .thenReturn(7L);
        when(dataSourceRepository.count()).thenReturn(3L);
        when(recordRepository.count()).thenReturn(500L);
        when(recordRepository.countByPushStatus(PushStatus.PUSHED)).thenReturn(350L);
        when(recordRepository.countByPushStatus(PushStatus.PENDING)).thenReturn(150L);

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
        when(recordRepository.aggregateTrendByDate(any())).thenReturn(List.of());

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
        when(recordRepository.aggregateTrendByDate(any())).thenReturn(List.of());

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
    void getTrend_onlyTodayHasData_shouldFillPrecedingDaysWithZero() {
        final String today = LocalDate.now().toString();
        // Repo returns one row for today: pushed=5, pending=2
        final List<Object[]> rows = List.<Object[]>of(new Object[] {today, 5L, 2L});
        when(recordRepository.aggregateTrendByDate(any())).thenReturn(rows);

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
        when(recordRepository.aggregateDistributionByMessageType(any(Pageable.class)))
                .thenReturn(rows);

        final List<DashboardDistributionItem> items = service.getDistribution("messageType");

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
        when(recordRepository.aggregateDistributionByBusinessType(any(Pageable.class)))
                .thenReturn(rows);

        final List<DashboardDistributionItem> items = service.getDistribution("businessType");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getName()).isEqualTo("BIZ_A");
        assertThat(items.get(0).getValue()).isEqualTo(30L);
        assertThat(items.get(1).getName()).isEqualTo("UNSPECIFIED");
        assertThat(items.get(1).getValue()).isEqualTo(10L);
    }

    @Test
    void getDistribution_invalidDim_shouldThrowIAE() {
        assertThatThrownBy(() -> service.getDistribution("foo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dim must be messageType or businessType");
    }
}
