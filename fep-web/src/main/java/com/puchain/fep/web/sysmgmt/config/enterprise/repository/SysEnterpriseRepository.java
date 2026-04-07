package com.puchain.fep.web.sysmgmt.config.enterprise.repository;

import com.puchain.fep.web.sysmgmt.config.enterprise.domain.SysEnterprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 企业主体 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysEnterpriseRepository extends JpaRepository<SysEnterprise, String> {

    /**
     * 按 USCI 查询企业主体。
     *
     * @param usci 统一社会信用代码
     * @return 企业主体（可为空）
     */
    Optional<SysEnterprise> findByUsci(String usci);

    /**
     * 按关键字和审核状态分页搜索企业主体。
     *
     * <p>关键字匹配企业名称或 USCI；auditStatus 为 null 时不过滤状态。</p>
     *
     * @param keyword     关键字（可为 null）
     * @param auditStatus 审核状态字符串（可为 null）
     * @param pageable    分页参数
     * @return 分页结果
     */
    @Query("SELECT e FROM SysEnterprise e WHERE "
            + "(:keyword IS NULL OR e.enterpriseName LIKE CONCAT('%', :keyword, '%') "
            + "OR e.usci LIKE CONCAT('%', :keyword, '%')) "
            + "AND (:auditStatus IS NULL OR CAST(e.auditStatus AS string) = :auditStatus) "
            + "ORDER BY e.createTime DESC")
    Page<SysEnterprise> search(@Param("keyword") String keyword,
                               @Param("auditStatus") String auditStatus,
                               Pageable pageable);
}
