package com.puchain.fep.web.sysmgmt.log.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * checkpoint 锚仓储（单行：findById(SINGLETON_ID) + save 覆盖；EFF-S5-1）。
 *
 * <p>不受 append-only ArchUnit 规则约束——该规则仅约束审计行表仓储
 * {@code SysOperationLogRepository}；checkpoint 锚语义本身即覆盖更新。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface AuditChainCheckpointRepository
        extends JpaRepository<AuditChainCheckpoint, String> {
}
