package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.dashboard.dto.DashboardResponse;
import com.puchain.fep.web.submission.datasource.repository.SubDataSourceRepository;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.repository.SubSubmissionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Submission dashboard statistics service.
 *
 * <p>Aggregates counts from output interfaces, data sources, and submission
 * records for the data overview page. See PRD v1.3 section 5.5.1
 * Data Overview (FR-WEB-SUB-DASH).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SubDashboardService {

    private static final Logger log = LoggerFactory.getLogger(SubDashboardService.class);

    private final SubOutputInterfaceRepository outputInterfaceRepository;
    private final SubDataSourceRepository dataSourceRepository;
    private final SubSubmissionRecordRepository recordRepository;

    /**
     * Constructs SubDashboardService.
     *
     * @param outputInterfaceRepository output interface repository
     * @param dataSourceRepository      data source repository
     * @param recordRepository          submission record repository
     */
    public SubDashboardService(
            final SubOutputInterfaceRepository outputInterfaceRepository,
            final SubDataSourceRepository dataSourceRepository,
            final SubSubmissionRecordRepository recordRepository) {
        this.outputInterfaceRepository = outputInterfaceRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.recordRepository = recordRepository;
    }

    /**
     * Returns aggregated dashboard statistics.
     *
     * @return dashboard response with interface, data source, and record counts
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        log.debug("Aggregating dashboard statistics");

        final DashboardResponse resp = new DashboardResponse();
        resp.setTotalInterfaceCount(outputInterfaceRepository.count());
        resp.setEnabledInterfaceCount(
                outputInterfaceRepository.countByInterfaceStatus(EnableDisableStatus.ENABLED));
        resp.setTotalDataSourceCount(dataSourceRepository.count());
        resp.setTotalRecordCount(recordRepository.count());
        resp.setPushedRecordCount(recordRepository.countByPushStatus(PushStatus.PUSHED));
        resp.setPendingRecordCount(recordRepository.countByPushStatus(PushStatus.PENDING));
        return resp;
    }
}
