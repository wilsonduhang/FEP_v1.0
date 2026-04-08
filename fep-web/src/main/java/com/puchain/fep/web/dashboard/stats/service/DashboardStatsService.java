package com.puchain.fep.web.dashboard.stats.service;

import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.domain.MessageProcessStatus;
import com.puchain.fep.web.bizdata.record.repository.BizMessageRecordRepository;
import com.puchain.fep.web.dashboard.stats.dto.DistributionItem;
import com.puchain.fep.web.dashboard.stats.dto.StatsCardsResponse;
import com.puchain.fep.web.dashboard.stats.dto.StatusDistributionItem;
import com.puchain.fep.web.dashboard.stats.dto.TimeRange;
import com.puchain.fep.web.dashboard.stats.dto.TrendDataPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for dashboard statistics aggregation.
 *
 * <p>Provides four types of statistics: summary cards, trend data,
 * message type distribution, and status distribution. All queries
 * are read-only. See PRD v1.3 section 5.2.3 (FR-WEB-DASH-STAT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class DashboardStatsService {

    private static final Logger LOG =
            LoggerFactory.getLogger(DashboardStatsService.class);

    private static final int HOURS_IN_DAY = 24;
    private static final int DAYS_IN_WEEK = 7;
    private static final double HUNDRED = 100.0;
    private static final int PERCENTAGE_SCALE = 1;
    private static final DateTimeFormatter TIME_LABEL =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_LABEL =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BizMessageRecordRepository messageRecordRepository;

    /**
     * Constructs DashboardStatsService.
     *
     * @param messageRecordRepository message record repository
     */
    public DashboardStatsService(
            final BizMessageRecordRepository messageRecordRepository) {
        this.messageRecordRepository = messageRecordRepository;
    }

    /**
     * Returns the four summary card values.
     *
     * @return stats cards with totalAmount, successCount,
     *         todayMessageCount, exceptionCount
     */
    @Transactional(readOnly = true)
    public StatsCardsResponse getCards() {
        LOG.debug("Aggregating dashboard stats cards");

        final LocalDate today = LocalDate.now();
        final LocalDateTime todayStart = today.atStartOfDay();
        final LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        final StatsCardsResponse resp = new StatsCardsResponse();
        final BigDecimal amount = messageRecordRepository.sumAmount();
        resp.setTotalAmount(amount != null ? amount : BigDecimal.ZERO);
        resp.setSuccessCount(messageRecordRepository
                .countByProcessStatus(MessageProcessStatus.SUCCESS));
        resp.setTodayMessageCount(messageRecordRepository
                .countByCreateTimeBetween(todayStart, todayEnd));
        resp.setExceptionCount(messageRecordRepository
                .countByProcessStatus(MessageProcessStatus.FAILED));
        return resp;
    }

    /**
     * Returns trend data points for the given time range.
     *
     * @param range time range (TODAY, THIS_WEEK, THIS_MONTH, CUSTOM)
     * @param start custom start date (only used when range is CUSTOM)
     * @param end   custom end date (only used when range is CUSTOM)
     * @return list of trend data points with sent/received counts
     */
    @Transactional(readOnly = true)
    public List<TrendDataPoint> getTrend(final TimeRange range,
                                         final LocalDate start,
                                         final LocalDate end) {
        LOG.debug("Aggregating trend data for range: {}", range);

        return switch (range) {
            case TODAY -> buildHourlyTrend(LocalDate.now());
            case THIS_WEEK -> buildDailyTrend(
                    LocalDate.now().minusDays(DAYS_IN_WEEK - 1),
                    LocalDate.now());
            case THIS_MONTH -> buildDailyTrend(
                    LocalDate.now().withDayOfMonth(1),
                    LocalDate.now());
            case CUSTOM -> buildDailyTrend(
                    start != null ? start : LocalDate.now(),
                    end != null ? end : LocalDate.now());
        };
    }

    /**
     * Returns message distribution grouped by message code.
     *
     * @return list of distribution items with percentage
     */
    @Transactional(readOnly = true)
    public List<DistributionItem> getDistribution() {
        LOG.debug("Aggregating message code distribution");

        final List<Object[]> rows =
                messageRecordRepository.countGroupByMessageCode();
        if (rows.isEmpty()) {
            return List.of();
        }

        final long total = rows.stream()
                .mapToLong(r -> (Long) r[1]).sum();

        final List<DistributionItem> items = new ArrayList<>(rows.size());
        for (final Object[] row : rows) {
            final String code = (String) row[0];
            final long count = (Long) row[1];
            final double pct = calcPercentage(count, total);
            items.add(new DistributionItem(code, code, count, pct));
        }
        return items;
    }

    /**
     * Returns status distribution across all process statuses.
     *
     * @return list of status distribution items with percentage
     */
    @Transactional(readOnly = true)
    public List<StatusDistributionItem> getStatusDistribution() {
        LOG.debug("Aggregating status distribution");

        final List<Object[]> rows =
                messageRecordRepository.countGroupByProcessStatus();

        final Map<String, Long> countMap = new HashMap<>();
        long total = 0;
        for (final Object[] row : rows) {
            final String status = ((MessageProcessStatus) row[0]).name();
            final long count = (Long) row[1];
            countMap.put(status, count);
            total += count;
        }

        final List<StatusDistributionItem> items =
                new ArrayList<>(MessageProcessStatus.values().length);
        for (final MessageProcessStatus status
                : MessageProcessStatus.values()) {
            final long count =
                    countMap.getOrDefault(status.name(), 0L);
            final double pct = calcPercentage(count, total);
            items.add(new StatusDistributionItem(
                    status.name(), count, pct));
        }
        return items;
    }

    private List<TrendDataPoint> buildHourlyTrend(final LocalDate day) {
        final List<TrendDataPoint> points =
                new ArrayList<>(HOURS_IN_DAY);
        for (int h = 0; h < HOURS_IN_DAY; h++) {
            final LocalDateTime slotStart =
                    day.atTime(h, 0, 0);
            final LocalDateTime slotEnd =
                    day.atTime(h, 59, 59, 999_999_999);
            final String label = slotStart.format(TIME_LABEL);
            final long sent = messageRecordRepository
                    .countByDirectionAndCreateTimeBetween(
                            MessageDirection.OUTBOUND,
                            slotStart, slotEnd);
            final long received = messageRecordRepository
                    .countByDirectionAndCreateTimeBetween(
                            MessageDirection.INBOUND,
                            slotStart, slotEnd);
            points.add(new TrendDataPoint(label, sent, received));
        }
        return points;
    }

    private List<TrendDataPoint> buildDailyTrend(
            final LocalDate from, final LocalDate to) {
        final List<TrendDataPoint> points = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            final LocalDateTime dayStart = cursor.atStartOfDay();
            final LocalDateTime dayEnd = cursor.atTime(LocalTime.MAX);
            final String label = cursor.format(DATE_LABEL);
            final long sent = messageRecordRepository
                    .countByDirectionAndCreateTimeBetween(
                            MessageDirection.OUTBOUND,
                            dayStart, dayEnd);
            final long received = messageRecordRepository
                    .countByDirectionAndCreateTimeBetween(
                            MessageDirection.INBOUND,
                            dayStart, dayEnd);
            points.add(new TrendDataPoint(label, sent, received));
            cursor = cursor.plusDays(1);
        }
        return points;
    }

    private double calcPercentage(final long count, final long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(HUNDRED))
                .divide(BigDecimal.valueOf(total),
                        PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
