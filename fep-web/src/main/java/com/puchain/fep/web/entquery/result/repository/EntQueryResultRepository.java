package com.puchain.fep.web.entquery.result.repository;

import com.puchain.fep.web.entquery.result.domain.EntQueryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 企业信息查询结果 Repository。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface EntQueryResultRepository extends JpaRepository<EntQueryResult, String> {

    /**
     * 按任务 ID 查询所有结果记录。
     *
     * @param taskId 查询任务 ID
     * @return 结果列表（可能为空）
     */
    List<EntQueryResult> findByTaskId(String taskId);
}
