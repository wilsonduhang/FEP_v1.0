package com.puchain.fep.web.submission.record.repository;

import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.domain.SubSubmissionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报送记录 Repository。
 *
 * <p>参见 PRD v1.3 §5.5.5 报文数据列表 + §5.6 报送管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SubSubmissionRecordRepository
        extends JpaRepository<SubSubmissionRecord, String> {

    /**
     * 按关键字 + 时间范围搜索报送记录（分页）。
     *
     * <p>关键字匹配 messageName 或 businessNo；时间范围过滤 createTime。</p>
     *
     * @param keyword   关键字（可为 null）
     * @param startTime 起始时间（可为 null）
     * @param endTime   截止时间（可为 null）
     * @param pageable  分页参数
     * @return 分页结果
     */
    @Query("SELECT r FROM SubSubmissionRecord r "
            + "WHERE (:keyword IS NULL "
            + "  OR r.messageName LIKE %:keyword% "
            + "  OR r.businessNo LIKE %:keyword%) "
            + "AND (:startTime IS NULL OR r.createTime >= :startTime) "
            + "AND (:endTime IS NULL OR r.createTime <= :endTime)")
    Page<SubSubmissionRecord> search(@Param("keyword") String keyword,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     Pageable pageable);

    /**
     * 按报文类型聚合统计（4 列：messageType, messageName, businessTypeId, totalCount）。
     *
     * @return 聚合结果列表（Object[] 数组）
     */
    @Query("SELECT r.messageType, r.messageName, r.businessTypeId, COUNT(r) "
            + "FROM SubSubmissionRecord r "
            + "GROUP BY r.messageType, r.messageName, r.businessTypeId")
    List<Object[]> aggregateByMessageType();

    /**
     * 按报文类型和推送状态统计数量。
     *
     * @param messageType 报文类型
     * @param pushStatus  推送状态
     * @return 数量
     */
    long countByMessageTypeAndPushStatus(String messageType, PushStatus pushStatus);

    /**
     * 批量统计已推送记录数（按报文类型分组）。
     *
     * @return 聚合结果列表（Object[] 数组：[messageType, count]）
     */
    @Query("SELECT r.messageType, COUNT(r) FROM SubSubmissionRecord r "
            + "WHERE r.pushStatus = com.puchain.fep.web.submission.record.domain.PushStatus.PUSHED "
            + "GROUP BY r.messageType")
    List<Object[]> countPushedGroupByMessageType();

    /**
     * 按报文类型查询记录（分页）。
     *
     * @param messageType 报文类型
     * @param pageable    分页参数
     * @return 分页结果
     */
    Page<SubSubmissionRecord> findByMessageType(String messageType, Pageable pageable);

    /**
     * 按报文类型统计总数。
     *
     * @param messageType 报文类型
     * @return 数量
     */
    long countByMessageType(String messageType);

    /**
     * 查询指定推送状态列表中的记录（用于获取阻塞记录）。
     *
     * @param pushStatuses 推送状态列表
     * @return 匹配的记录列表
     */
    List<SubSubmissionRecord> findByPushStatusIn(List<PushStatus> pushStatuses);

    /**
     * 查询指定推送状态列表中的记录（分页）。
     *
     * @param pushStatuses 推送状态列表
     * @param pageable     分页参数
     * @return 分页结果
     */
    Page<SubSubmissionRecord> findByPushStatusIn(List<PushStatus> pushStatuses,
                                                  Pageable pageable);

    /**
     * 按推送状态和记录 ID 列表查询（用于批量推送）。
     *
     * @param pushStatus 推送状态
     * @param recordIds  记录 ID 列表
     * @return 匹配的记录列表
     */
    List<SubSubmissionRecord> findByPushStatusAndRecordIdIn(PushStatus pushStatus,
                                                            List<String> recordIds);

    /**
     * 按推送状态统计数量（用于数据概况）。
     *
     * @param pushStatus 推送状态
     * @return 数量
     */
    long countByPushStatus(PushStatus pushStatus);

    /**
     * 按推送状态聚合全量 record 的数量统计（用于 Dashboard 概况面板）。
     *
     * <p>合并 {@code count()} / {@code countByPushStatus(PUSHED)} / {@code countByPushStatus(PENDING)}
     * 为单次查询，将 Dashboard 页三次 COUNT 往返压至一次。返回 {@link List}（保证至少 1 行；
     * 空表时 pushed/pending 列为 {@code null}，调用方需做 null-safe 处理）。</p>
     *
     * @return 单行聚合结果 {@code [totalCount, pushedCount, pendingCount]}，外层包一层 List
     */
    @Query("SELECT COUNT(r), "
            + "SUM(CASE WHEN r.pushStatus = "
            + "com.puchain.fep.web.submission.record.domain.PushStatus.PUSHED "
            + "THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN r.pushStatus = "
            + "com.puchain.fep.web.submission.record.domain.PushStatus.PENDING "
            + "THEN 1L ELSE 0L END) "
            + "FROM SubSubmissionRecord r")
    List<Object[]> aggregatePushStatusCounts();

    /**
     * 按报文类型统计趋势（按月聚合，H2 兼容 JPQL）。
     *
     * @param messageType 报文类型
     * @return 趋势数据列表（Object[] 数组：[period, count]）
     */
    @Query("SELECT SUBSTRING(CAST(r.createTime AS string), 1, 7), COUNT(r) "
            + "FROM SubSubmissionRecord r "
            + "WHERE r.messageType = :messageType "
            + "GROUP BY SUBSTRING(CAST(r.createTime AS string), 1, 7) "
            + "ORDER BY SUBSTRING(CAST(r.createTime AS string), 1, 7)")
    List<Object[]> trendByMessageType(@Param("messageType") String messageType);

    /**
     * 按日粒度聚合 pushed / pending 计数（用于 Dashboard 趋势图）。
     *
     * <p>延用 {@code SUBSTRING(CAST(createTime AS string), 1, 10)} 日截断模式。
     * pushed 仅 PUSHED；pending 仅 PENDING（PUSHING/FAILED 不计入）。</p>
     *
     * <p>窗口为半开区间 {@code [startTime, endExclusive)}；调用方捕获一次
     * {@link java.time.LocalDate#now()} 后同时传入两端，避免调用 SQL 与填充
     * dates 列表之间跨过午夜造成最后一格错位，或请求发出之后产生的行落入显示窗口外。</p>
     *
     * @param startTime     起始时间（含）
     * @param endExclusive  截止时间（不含，通常为 today+1 零点）
     * @return 聚合结果列表（Object[] 数组：[date(yyyy-MM-dd), pushedCount, pendingCount]）
     */
    @Query("SELECT SUBSTRING(CAST(r.createTime AS string), 1, 10), "
            + "SUM(CASE WHEN r.pushStatus = "
            + "com.puchain.fep.web.submission.record.domain.PushStatus.PUSHED "
            + "THEN 1L ELSE 0L END), "
            + "SUM(CASE WHEN r.pushStatus = "
            + "com.puchain.fep.web.submission.record.domain.PushStatus.PENDING "
            + "THEN 1L ELSE 0L END) "
            + "FROM SubSubmissionRecord r "
            + "WHERE r.createTime >= :startTime AND r.createTime < :endExclusive "
            + "GROUP BY SUBSTRING(CAST(r.createTime AS string), 1, 10) "
            + "ORDER BY SUBSTRING(CAST(r.createTime AS string), 1, 10)")
    List<Object[]> aggregateTrendByDate(@Param("startTime") LocalDateTime startTime,
                                        @Param("endExclusive") LocalDateTime endExclusive);

    /**
     * 按报文类型分布聚合 Top N，支持可选时间下限过滤。
     *
     * <p>用 Pageable 做 LIMIT；JPQL 无 LIMIT 关键字。</p>
     *
     * @param startTime 非 null 时仅统计 {@code create_time >= startTime} 的记录；null 为全量
     * @param pageable  分页参数（通常 PageRequest.of(0, 10) 取 Top 10）
     * @return 聚合结果列表（Object[] 数组：[messageType, count]，按 count 降序）
     */
    @Query("SELECT r.messageType, COUNT(r) FROM SubSubmissionRecord r "
            + "WHERE (:startTime IS NULL OR r.createTime >= :startTime) "
            + "GROUP BY r.messageType ORDER BY COUNT(r) DESC")
    List<Object[]> aggregateDistributionByMessageType(
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);

    /**
     * 按业务类型分布聚合 Top N；null businessTypeId 映射为 {@code 'UNSPECIFIED'}。
     *
     * @param startTime 非 null 时仅统计 {@code create_time >= startTime} 的记录；null 为全量
     * @param pageable  分页参数（通常 PageRequest.of(0, 10) 取 Top 10）
     * @return 聚合结果列表（Object[] 数组：[businessTypeId, count]，按 count 降序）
     */
    @Query("SELECT COALESCE(r.businessTypeId, 'UNSPECIFIED'), COUNT(r) "
            + "FROM SubSubmissionRecord r "
            + "WHERE (:startTime IS NULL OR r.createTime >= :startTime) "
            + "GROUP BY COALESCE(r.businessTypeId, 'UNSPECIFIED') "
            + "ORDER BY COUNT(r) DESC")
    List<Object[]> aggregateDistributionByBusinessType(
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);
}
