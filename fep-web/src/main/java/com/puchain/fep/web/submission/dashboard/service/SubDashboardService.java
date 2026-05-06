package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.submission.dashboard.dto.DashboardDistributionItem;
import com.puchain.fep.web.submission.dashboard.dto.DashboardResponse;
import com.puchain.fep.web.submission.dashboard.dto.DashboardTrendResponse;
import com.puchain.fep.web.submission.datasource.repository.SubDataSourceRepository;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.submission.record.repository.SubSubmissionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** Distribution endpoint Top-N cap. */
    private static final int TOP_N = 10;

    /** Trend endpoint allowed lookback window (in days) — 7. */
    private static final int TREND_DAYS_7 = 7;

    /** Trend endpoint allowed lookback window (in days) — 30. */
    private static final int TREND_DAYS_30 = 30;

    /** Distribution endpoint default lookback window when {@code days} omitted. */
    private static final int DISTRIBUTION_DAYS_DEFAULT = 90;

    /** Distribution endpoint minimum lookback window (inclusive). */
    private static final int DISTRIBUTION_DAYS_MIN = 1;

    /** Distribution endpoint maximum lookback window (inclusive). */
    private static final int DISTRIBUTION_DAYS_MAX = 365;

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
     * <p>使用两个聚合查询 + 一个简单 count 合并原 6 次 COUNT 往返为 3 次：
     * <ul>
     *   <li>{@code SubOutputInterfaceRepository.aggregateInterfaceCounts} —
     *       total + enabled</li>
     *   <li>{@code SubDataSourceRepository.count} — 保留原 count</li>
     *   <li>{@code SubSubmissionRecordRepository.aggregatePushStatusCounts} —
     *       total + pushed + pending（pending 仅 {@code PushStatus.PENDING}，
     *       不含 PUSHING/FAILED，与 getTrend 语义一致）</li>
     * </ul>
     *
     * @return dashboard response with interface, data source, and record counts
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        log.debug("Aggregating dashboard statistics");

        final DashboardResponse resp = new DashboardResponse();

        // 聚合 JPQL 单行查询的 Spring Data 签名要求 List<Object[]>，取首行
        final Object[] ifaceCounts = outputInterfaceRepository.aggregateInterfaceCounts().get(0);
        resp.setTotalInterfaceCount(toLong(ifaceCounts[0]));
        resp.setEnabledInterfaceCount(toLong(ifaceCounts[1]));

        resp.setTotalDataSourceCount(dataSourceRepository.count());

        final Object[] recordCounts = recordRepository.aggregatePushStatusCounts().get(0);
        resp.setTotalRecordCount(toLong(recordCounts[0]));
        resp.setPushedRecordCount(toLong(recordCounts[1]));
        // pendingRecordCount 仅统计 PushStatus.PENDING，不含 PUSHING/FAILED
        resp.setPendingRecordCount(toLong(recordCounts[2]));

        return resp;
    }

    /**
     * 将聚合查询返回的 SUM 结果安全转 long（空表时 SUM 返回 null）。
     *
     * @param value 聚合结果单元（可能为 null）
     * @return 非 null 的 long 值，null 转 0
     */
    private static long toLong(final Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    /**
     * 按日粒度返回推送趋势（pushed / pending 双系列）。
     *
     * <p>空日补 0；pushed 仅 PUSHED，pending 仅 PENDING（PUSHING/FAILED 不计入，
     * 与 {@link #getDashboard()} pendingRecordCount 语义一致）。</p>
     *
     * @param days 7 或 30
     * @return 趋势响应（日期数 = days；pushedCounts / pendingCounts 等长）
     * @throws IllegalArgumentException days 非 7/30（Handler 自动转 400 PARAM_4002）
     */
    @Transactional(readOnly = true)
    public DashboardTrendResponse getTrend(final int days) {
        if (days != TREND_DAYS_7 && days != TREND_DAYS_30) {
            throw new IllegalArgumentException("days must be 7 or 30");
        }
        log.debug("Aggregating dashboard trend: days={}", days);

        // Capture LocalDate once so the half-open [start, endExclusive) window
        // passed to the repo query is identical to the window used to fill the
        // dates list — avoids a midnight tick between SQL and loop skewing the
        // last bucket, and excludes rows that arrive after the request started.
        final LocalDate today = LocalDate.now();
        final LocalDate start = today.minusDays((long) days - 1L);
        final LocalDateTime startAt = start.atStartOfDay();
        final LocalDateTime endExclusive = today.plusDays(1L).atStartOfDay();
        final Map<String, long[]> byDate = new HashMap<>();
        for (Object[] row : recordRepository.aggregateTrendByDate(startAt, endExclusive)) {
            final String dateStr = (String) row[0];
            byDate.put(dateStr, new long[] {
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
            });
        }

        final List<String> dates = new ArrayList<>(days);
        final List<Long> pushed = new ArrayList<>(days);
        final List<Long> pending = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            final String d = start.plusDays(i).toString();
            dates.add(d);
            final long[] counts = byDate.getOrDefault(d, new long[] {0L, 0L});
            pushed.add(counts[0]);
            pending.add(counts[1]);
        }

        final DashboardTrendResponse resp = new DashboardTrendResponse();
        resp.setDates(dates);
        resp.setPushedCounts(pushed);
        resp.setPendingCounts(pending);
        return resp;
    }

    /**
     * 按维度返回分布 Top 10，支持可选时间窗。
     *
     * <p>支持维度：{@code "messageType"} / {@code "businessType"}。
     * businessType 维度下 {@code null} 业务类型聚合为 {@code "UNSPECIFIED"}。</p>
     *
     * <p>时间窗：{@code days} 为 {@code null} 时使用默认 {@value #DISTRIBUTION_DAYS_DEFAULT}
     * 天回看（PRD v1.3 §5.5.1 D-2 决议）；非空时必须落在
     * {@code [DISTRIBUTION_DAYS_MIN, DISTRIBUTION_DAYS_MAX]} 闭区间内。
     * 校验顺序：先校验 dim，再校验 days，与 PRD 契约一致。</p>
     *
     * @param dim  维度（messageType / businessType）
     * @param days 回看天数（可选，null 时默认 90；越界抛 IAE）
     * @return Top 10 分布项（按 value 降序）
     * @throws IllegalArgumentException dim 非法或 days 越界
     *                                  （Handler 自动转 400 PARAM_4002）
     */
    @Transactional(readOnly = true)
    public List<DashboardDistributionItem> getDistribution(
            final String dim, final Integer days) {
        // dim 校验先于 days 校验，确保契约一致（invalidDim+days=0 仍报 dim 错）
        if (!"messageType".equals(dim) && !"businessType".equals(dim)) {
            throw new IllegalArgumentException(
                    "dim must be messageType or businessType");
        }
        final int effectiveDays = (days == null) ? DISTRIBUTION_DAYS_DEFAULT : days;
        if (effectiveDays < DISTRIBUTION_DAYS_MIN
                || effectiveDays > DISTRIBUTION_DAYS_MAX) {
            throw new IllegalArgumentException(
                    "days must be between " + DISTRIBUTION_DAYS_MIN
                            + " and " + DISTRIBUTION_DAYS_MAX);
        }
        final LocalDateTime startTime =
                LocalDate.now().minusDays(effectiveDays).atStartOfDay();
        log.debug("Aggregating dashboard distribution: dim={} days={}",
                LogSanitizer.sanitize(dim), effectiveDays);
        final List<Object[]> raw = switch (dim) {
            case "messageType" -> recordRepository.aggregateDistributionByMessageType(
                    startTime, PageRequest.of(0, TOP_N));
            case "businessType" -> recordRepository.aggregateDistributionByBusinessType(
                    startTime, PageRequest.of(0, TOP_N));
            default -> throw new IllegalStateException("unreachable");
        };
        return raw.stream().map(r -> {
            final DashboardDistributionItem item = new DashboardDistributionItem();
            item.setName((String) r[0]);
            item.setValue(((Number) r[1]).longValue());
            return item;
        }).toList();
    }

    /**
     * 单参 overload：维持 R4 之前调用方兼容性（无活跃生产 callsite）。
     *
     * @param dim 维度（messageType / businessType）
     * @return Top 10 分布项（默认 90 天窗口）
     * @throws IllegalArgumentException dim 非法
     * @deprecated 改用 {@link #getDistribution(String, Integer)} 显式传 days；
     *             R5 移除（{@code forRemoval=true}）。
     */
    @Deprecated(since = "R4", forRemoval = true)
    @Transactional(readOnly = true)
    public List<DashboardDistributionItem> getDistribution(final String dim) {
        return getDistribution(dim, null);
    }
}
