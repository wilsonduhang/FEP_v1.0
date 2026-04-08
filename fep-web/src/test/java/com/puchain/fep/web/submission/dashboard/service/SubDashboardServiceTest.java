package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.dashboard.dto.DashboardResponse;
import com.puchain.fep.web.submission.datasource.repository.SubDataSourceRepository;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.repository.SubSubmissionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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
}
