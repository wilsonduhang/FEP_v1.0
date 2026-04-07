package com.puchain.fep.web.sysmgmt.config.enterprise.repository;

import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterpriseBiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 企业业务信息关联 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysEnterpriseBizRepository extends JpaRepository<SysEnterpriseBiz, String> {

    /**
     * 按企业主体 ID 查询所有业务关联记录。
     *
     * @param enterpriseId 企业主体 ID
     * @return 业务关联列表
     */
    List<SysEnterpriseBiz> findByEnterpriseId(String enterpriseId);
}
