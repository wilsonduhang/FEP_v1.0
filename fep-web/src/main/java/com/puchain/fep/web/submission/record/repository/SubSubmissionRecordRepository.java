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
}
