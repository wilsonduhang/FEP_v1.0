package com.puchain.fep.web.sysmgmt.config.businesstype.repository;

import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 业务类型 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysBusinessTypeRepository extends JpaRepository<SysBusinessType, String> {

    /**
     * 判断业务类型编码是否已存在。
     *
     * @param typeCode 业务类型编码
     * @return true 已存在
     */
    boolean existsByTypeCode(String typeCode);

    /**
     * 判断编码是否被其他记录使用（排除指定 ID）。
     *
     * @param typeCode 业务类型编码
     * @param typeId   要排除的业务类型 ID
     * @return true 已被其他记录使用
     */
    boolean existsByTypeCodeAndTypeIdNot(String typeCode, String typeId);

    /**
     * 按类型名称模糊搜索（分页）。
     *
     * @param keyword  名称关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SysBusinessType> findByTypeNameContaining(String keyword, Pageable pageable);
}
