package com.puchain.fep.web.sysmgmt.config.outputtype.repository;

import com.puchain.fep.web.sysmgmt.config.outputtype.domain.SysOutputType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 输出类型 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysOutputTypeRepository extends JpaRepository<SysOutputType, String> {

    /**
     * 判断输出类型编码是否已存在。
     *
     * @param typeCode 输出类型编码
     * @return true 已存在
     */
    boolean existsByTypeCode(String typeCode);

    /**
     * 判断编码是否被其他记录使用（排除指定 ID）。
     *
     * @param typeCode     输出类型编码
     * @param outputTypeId 要排除的输出类型 ID
     * @return true 已被其他记录使用
     */
    boolean existsByTypeCodeAndOutputTypeIdNot(String typeCode, String outputTypeId);

    /**
     * 按类型名称模糊搜索（分页）。
     *
     * @param keyword  名称关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SysOutputType> findByTypeNameContaining(String keyword, Pageable pageable);
}
