package com.puchain.fep.web.sysmgmt.log.repository;

import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 操作日志 Repository。
 *
 * <p>参见 PRD v1.3 §5.10.6 日志管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysOperationLogRepository extends JpaRepository<SysOperationLog, String> {

    /**
     * 多条件分页查询操作日志。
     *
     * <p>所有过滤参数均为可选（传 null 表示不过滤该字段）。</p>
     *
     * @param userAccount 操作人账号（模糊匹配），可为 null
     * @param module      功能模块（精确匹配），可为 null
     * @param startTime   操作时间起始（含），可为 null
     * @param endTime     操作时间截止（含），可为 null
     * @param traceId     链路追踪 ID（精确匹配，GM S5 架构 §1219），可为 null
     * @param pageable    分页参数
     * @return 分页结果
     */
    @Query("SELECT l FROM SysOperationLog l WHERE "
            + "(:userAccount IS NULL OR l.userAccount LIKE CONCAT('%', :userAccount, '%')) "
            + "AND (:module IS NULL OR l.module = :module) "
            + "AND (:startTime IS NULL OR l.createTime >= :startTime) "
            + "AND (:endTime IS NULL OR l.createTime <= :endTime) "
            + "AND (:traceId IS NULL OR l.traceId = :traceId) "
            + "ORDER BY l.createTime DESC")
    Page<SysOperationLog> search(@Param("userAccount") String userAccount,
                                 @Param("module") String module,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime,
                                 @Param("traceId") String traceId,
                                 Pageable pageable);

    /**
     * 链尾行（seq 最大且非 null；GM S5 AuditChainWriter 启动恢复用）。
     *
     * @return 链尾行；空链时 empty
     */
    Optional<SysOperationLog> findTopBySeqIsNotNullOrderBySeqDesc();

    /**
     * 按链 seq 读单行（EFF-S5-1 checkpoint 锚行一致性校验用）。
     *
     * @param seq 链序号
     * @return 该 seq 行；不存在 empty
     */
    Optional<SysOperationLog> findBySeq(Long seq);

    /**
     * 自 seq 起链上行分页升序读取（EFF-S5-1 incremental 锚后增量段扫描用）。
     *
     * @param seq      起始链序号（含）
     * @param pageable 分页参数
     * @return 链上行（seq 升序）
     */
    Page<SysOperationLog> findBySeqGreaterThanEqualOrderBySeqAsc(Long seq, Pageable pageable);
}
