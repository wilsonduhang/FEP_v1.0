package com.puchain.fep.web.entquery.task.repository;

import com.puchain.fep.web.entquery.task.domain.EntQueryTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 企业信息查询任务 Repository。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface EntQueryTaskRepository extends JpaRepository<EntQueryTask, String> {

    /**
     * 按查询类型、任务状态、关键字分页搜索查询任务。
     *
     * <p>关键字匹配被查询企业 USCI 或名称；queryType/taskStatus 为 null 时不过滤。</p>
     *
     * @param queryType  查询类型字符串（可为 null）
     * @param taskStatus 任务状态字符串（可为 null）
     * @param keyword    关键字（可为 null）
     * @param pageable   分页参数
     * @return 分页结果
     */
    @Query("SELECT t FROM EntQueryTask t WHERE "
            + "(:queryType IS NULL OR CAST(t.queryType AS string) = :queryType) "
            + "AND (:taskStatus IS NULL OR CAST(t.taskStatus AS string) = :taskStatus) "
            + "AND (:keyword IS NULL OR t.usci LIKE CONCAT('%', :keyword, '%') "
            + "OR t.queryTargetName LIKE CONCAT('%', :keyword, '%'))")
    Page<EntQueryTask> search(@Param("queryType") String queryType,
                              @Param("taskStatus") String taskStatus,
                              @Param("keyword") String keyword,
                              Pageable pageable);
}
