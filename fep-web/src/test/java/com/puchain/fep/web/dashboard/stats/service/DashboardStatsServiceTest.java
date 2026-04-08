package com.puchain.fep.web.dashboard.stats.service;

import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.domain.MessageProcessStatus;
import com.puchain.fep.web.bizdata.record.repository.BizMessageRecordRepository;
import com.puchain.fep.web.dashboard.stats.dto.DistributionItem;
import com.puchain.fep.web.dashboard.stats.dto.StatsCardsResponse;
import com.puchain.fep.web.dashboard.stats.dto.StatusDistributionItem;
import com.puchain.fep.web.dashboard.stats.dto.TimeRange;
import com.puchain.fep.web.dashboard.stats.dto.TrendDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardStatsService}.
 *
 * <p>Covers all four dashboard endpoints: cards, trend, distribution,
 * and status distribution. See PRD v1.3 section 5.2.3 (FR-WEB-DASH-STAT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DashboardStatsServiceTest {

    private static final int HOURS_IN_DAY = 24;
    private static final int DAYS_IN_WEEK = 7;
    private static final double PERCENTAGE_60 = 60.0;
    private static final double PERCENTAGE_40 = 40.0;
    private static final double PERCENTAGE_70 = 70.0;
    private static final long COUNT_120 = 120L;
    private static final long COUNT_35 = 35L;
    private static final long COUNT_8 = 8L;
    private static final long COUNT_70 = 70L;
    private static final long COUNT_20 = 20L;
    private static final long COUNT_5 = 5L;
    private static final long COUNT_60 = 60L;
    private static final long COUNT_40 = 40L;
    private static final int STATUS_COUNT = 4;

    @Mock
    private BizMessageRecordRepository messageRecordRepository;

    private DashboardStatsService service;

    @BeforeEach
    void setUp() {
        service = new DashboardStatsService(messageRecordRepository);
    }

    @Test
    void getCards_withNoData_shouldReturnAllZeros() {
        when(messageRecordRepository.sumAmount()).thenReturn(null);
        when(messageRecordRepository.countByProcessStatus(
                MessageProcessStatus.SUCCESS)).thenReturn(0L);
        when(messageRecordRepository.countByCreateTimeBetween(
                any(), any())).thenReturn(0L);
        when(messageRecordRepository.countByProcessStatus(
                MessageProcessStatus.FAILED)).thenReturn(0L);

        final StatsCardsResponse cards = service.getCards();

        assertThat(cards.getTotalAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cards.getSuccessCount()).isZero();
        assertThat(cards.getTodayMessageCount()).isZero();
        assertThat(cards.getExceptionCount()).isZero();
    }

    @Test
    void getCards_withData_shouldAggregateAllFourCards() {
        final BigDecimal amount = new BigDecimal("5000000.00");
        when(messageRecordRepository.sumAmount()).thenReturn(amount);
        when(messageRecordRepository.countByProcessStatus(
                MessageProcessStatus.SUCCESS)).thenReturn(COUNT_120);
        when(messageRecordRepository.countByCreateTimeBetween(
                any(), any())).thenReturn(COUNT_35);
        when(messageRecordRepository.countByProcessStatus(
                MessageProcessStatus.FAILED)).thenReturn(COUNT_8);

        final StatsCardsResponse cards = service.getCards();

        assertThat(cards.getTotalAmount())
                .isEqualByComparingTo(amount);
        assertThat(cards.getSuccessCount()).isEqualTo(COUNT_120);
        assertThat(cards.getTodayMessageCount()).isEqualTo(COUNT_35);
        assertThat(cards.getExceptionCount()).isEqualTo(COUNT_8);
    }

    @Test
    void getTrend_withToday_shouldReturn24HourlyPoints() {
        when(messageRecordRepository.countByDirectionAndCreateTimeBetween(
                any(MessageDirection.class), any(), any())).thenReturn(0L);

        final List<TrendDataPoint> points =
                service.getTrend(TimeRange.TODAY, null, null);

        assertThat(points).hasSize(HOURS_IN_DAY);
        assertThat(points.get(0).getLabel()).isEqualTo("00:00");
        assertThat(points.get(HOURS_IN_DAY - 1).getLabel())
                .isEqualTo("23:00");
        assertThat(points).allSatisfy(p -> {
            assertThat(p.getSentCount()).isZero();
            assertThat(p.getReceivedCount()).isZero();
        });
    }

    @Test
    void getTrend_withThisWeek_shouldReturn7DailyPoints() {
        when(messageRecordRepository.countByDirectionAndCreateTimeBetween(
                any(MessageDirection.class), any(), any())).thenReturn(0L);

        final List<TrendDataPoint> points =
                service.getTrend(TimeRange.THIS_WEEK, null, null);

        assertThat(points).hasSize(DAYS_IN_WEEK);
    }

    @Test
    void getDistribution_withNoRecords_shouldReturnEmptyList() {
        when(messageRecordRepository.countGroupByMessageCode())
                .thenReturn(Collections.emptyList());

        final List<DistributionItem> items = service.getDistribution();

        assertThat(items).isEmpty();
    }

    @Test
    void getDistribution_withRecords_shouldCalculatePercentage() {
        final List<Object[]> rows = List.of(
                new Object[]{"3000", COUNT_60},
                new Object[]{"3101", COUNT_40}
        );
        when(messageRecordRepository.countGroupByMessageCode())
                .thenReturn(rows);

        final List<DistributionItem> items = service.getDistribution();

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getMessageCode()).isEqualTo("3000");
        assertThat(items.get(0).getCount()).isEqualTo(COUNT_60);
        assertThat(items.get(0).getPercentage()).isEqualTo(PERCENTAGE_60);
        assertThat(items.get(1).getMessageCode()).isEqualTo("3101");
        assertThat(items.get(1).getCount()).isEqualTo(COUNT_40);
        assertThat(items.get(1).getPercentage()).isEqualTo(PERCENTAGE_40);
    }

    @Test
    void getStatusDistribution_shouldReturnAllStatusesWithCounts() {
        when(messageRecordRepository.countByProcessStatus(
                MessageProcessStatus.SUCCESS)).thenReturn(COUNT_70);
        when(messageRecordRepository.countByProcessStatus(
                MessageProcessStatus.FAILED)).thenReturn(COUNT_20);
        when(messageRecordRepository.countByProcessStatus(
                MessageProcessStatus.PROCESSING)).thenReturn(COUNT_5);
        when(messageRecordRepository.countByProcessStatus(
                MessageProcessStatus.PENDING)).thenReturn(COUNT_5);

        final List<StatusDistributionItem> items =
                service.getStatusDistribution();

        assertThat(items).hasSize(STATUS_COUNT);
        final long totalCount = items.stream()
                .mapToLong(StatusDistributionItem::getCount).sum();
        assertThat(totalCount).isEqualTo(100L);

        final StatusDistributionItem successItem = items.stream()
                .filter(i -> "SUCCESS".equals(i.getStatus()))
                .findFirst()
                .orElseThrow();
        assertThat(successItem.getPercentage())
                .isEqualTo(PERCENTAGE_70);
    }
}
