package com.puchain.fep.web.sysmgmt.config.enterprise.repository;

import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterpriseQueryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 企业精准查询配置 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysEnterpriseQueryConfigRepository extends JpaRepository<SysEnterpriseQueryConfig, String> {

    /**
     * 按企业主体 ID 查询精准查询配置（每个企业最多一条）。
     *
     * @param enterpriseId 企业主体 ID
     * @return 查询配置（可能为空）
     */
    Optional<SysEnterpriseQueryConfig> findByEnterpriseId(String enterpriseId);
}
