package com.puchain.fep.web.bizdata.record.repository;

import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.domain.BizMessageRecord;
import com.puchain.fep.web.bizdata.record.domain.MessageProcessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for {@link BizMessageRecord}.
 *
 * <p>Provides search, aggregation, and dashboard statistics queries.
 * See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface BizMessageRecordRepository
        extends JpaRepository<BizMessageRecord, String> {

    /**
     * Check if a serial number already exists.
     *
     * @param serialNo serial number
     * @return true if exists
     */
    boolean existsBySerialNo(String serialNo);

    /**
     * Search records with optional filters.
     *
     * @param messageCode  message code (null for all)
     * @param processStatus process status (null for all)
     * @param direction    message direction (null for all)
     * @param startTime    start time (null for no lower bound)
     * @param endTime      end time (null for no upper bound)
     * @param pageable     pagination params
     * @return paginated results
     */
    @Query("SELECT r FROM BizMessageRecord r "
            + "WHERE (:messageCode IS NULL OR r.messageCode = :messageCode) "
            + "AND (:processStatus IS NULL OR r.processStatus = :processStatus) "
            + "AND (:direction IS NULL OR r.direction = :direction) "
            + "AND (:startTime IS NULL OR r.createTime >= :startTime) "
            + "AND (:endTime IS NULL OR r.createTime <= :endTime)")
    Page<BizMessageRecord> search(
            @Param("messageCode") String messageCode,
            @Param("processStatus") MessageProcessStatus processStatus,
            @Param("direction") MessageDirection direction,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * Get message summary aggregated by message code.
     *
     * <p>Returns rows of [messageCode, totalCount, successCount,
     * pendingCount, failedCount].</p>
     *
     * @return aggregation result
     */
    @Query("SELECT r.messageCode, COUNT(r), "
            + "SUM(CASE WHEN r.processStatus = 'SUCCESS' THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN r.processStatus = 'PENDING' THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN r.processStatus = 'FAILED' THEN 1 ELSE 0 END) "
            + "FROM BizMessageRecord r GROUP BY r.messageCode")
    List<Object[]> getMessageSummary();

    // ===== Dashboard statistics queries (for Task 3) =====

    /**
     * Count records by process status.
     *
     * @param status process status
     * @return count
     */
    long countByProcessStatus(MessageProcessStatus status);

    /**
     * Count records created within a time range.
     *
     * @param start start time (inclusive)
     * @param end   end time (inclusive)
     * @return count
     */
    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count records by direction within a time range.
     *
     * @param direction message direction
     * @param start     start time (inclusive)
     * @param end       end time (inclusive)
     * @return count
     */
    long countByDirectionAndCreateTimeBetween(
            MessageDirection direction,
            LocalDateTime start,
            LocalDateTime end);

    /**
     * Sum total amount of all records.
     *
     * @return total amount (null if no records)
     */
    @Query("SELECT SUM(r.amount) FROM BizMessageRecord r")
    java.math.BigDecimal sumAmount();

    /**
     * Count records grouped by message code.
     *
     * <p>Returns rows of [messageCode, count].</p>
     *
     * @return grouping result
     */
    @Query("SELECT r.messageCode, COUNT(r) "
            + "FROM BizMessageRecord r GROUP BY r.messageCode")
    List<Object[]> countGroupByMessageCode();
}
