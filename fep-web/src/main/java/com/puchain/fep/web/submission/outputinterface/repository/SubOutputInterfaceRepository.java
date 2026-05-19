package com.puchain.fep.web.submission.outputinterface.repository;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    /**
     * 查指定 businessType 集合下处于给定状态的输出接口（回调 fan-out 解析）。
     *
     * @param businessTypeIds 业务类型 id 集合，非空
     * @param interfaceStatus 接口状态过滤
     * @return 命中接口列表，可能为空
     */
    List<SubOutputInterface> findByBusinessTypeIdInAndInterfaceStatus(
            List<String> businessTypeIds, EnableDisableStatus interfaceStatus);

    /**
     * 合并 total / enabled 为单次聚合查询（用于 Dashboard 概况面板）。
     *
     * <p>避免 Dashboard 页每次连发 {@code count()} + {@code countByInterfaceStatus()}
     * 两次 COUNT 查询。返回 {@link List}（保证至少 1 行；空表时 enabled 列为 {@code null}，
     * 调用方需做 null-safe 处理）。</p>
     *
     * @return 单行聚合结果 {@code [totalCount, enabledCount]}，外层包一层 List
     *         (Spring Data JPA 对聚合单行查询的签名要求)
     */
    @Query("SELECT COUNT(o), "
            + "SUM(CASE WHEN o.interfaceStatus = "
            + "com.puchain.fep.common.domain.EnableDisableStatus.ENABLED "
            + "THEN 1L ELSE 0L END) "
            + "FROM SubOutputInterface o")
    List<Object[]> aggregateInterfaceCounts();
}
