package com.puchain.fep.web.submission.outputinterface.repository;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 输出接口 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SubOutputInterfaceRepository extends JpaRepository<SubOutputInterface, String> {

    /**
     * 按接口名称判断是否存在。
     *
     * @param interfaceName 接口名称
     * @return 是否存在
     */
    boolean existsByInterfaceName(String interfaceName);

    /**
     * 按接口名称判断是否存在（排除指定 ID）。
     *
     * @param interfaceName 接口名称
     * @param interfaceId   排除的接口 ID
     * @return 是否存在
     */
    @Query("SELECT COUNT(o) > 0 FROM SubOutputInterface o "
            + "WHERE o.interfaceName = :name AND o.interfaceId <> :id")
    boolean existsByInterfaceNameAndIdNot(@Param("name") String interfaceName,
                                          @Param("id") String interfaceId);

    /**
     * 按关键字模糊搜索（接口名称），分页返回。
     *
     * @param keyword  关键字（可为 null）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT o FROM SubOutputInterface o "
            + "WHERE (:keyword IS NULL OR o.interfaceName LIKE %:keyword%)")
    Page<SubOutputInterface> search(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 按业务类型 ID 统计接口数量。
     *
     * @param businessTypeId 业务类型 ID
     * @return 接口数量
     */
    long countByBusinessTypeId(String businessTypeId);

    /**
     * 按接口状态统计数量（用于数据概况统计）。
     *
     * @param status 接口状态
     * @return 接口数量
     */
    long countByInterfaceStatus(EnableDisableStatus status);
}
