package com.puchain.fep.web.sysmgmt.config.pushinterface.repository;

import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.SysPushInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 推送接口 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysPushInterfaceRepository extends JpaRepository<SysPushInterface, String> {

    /**
     * 判断接口名称是否已存在。
     *
     * @param interfaceName 接口名称
     * @return true 已存在
     */
    boolean existsByInterfaceName(String interfaceName);

    /**
     * 判断名称是否被其他记录使用（排除指定 ID）。
     *
     * @param interfaceName 接口名称
     * @param interfaceId   要排除的接口 ID
     * @return true 已被其他记录使用
     */
    boolean existsByInterfaceNameAndInterfaceIdNot(String interfaceName, String interfaceId);

    /**
     * 按接口名称模糊搜索（分页）。
     *
     * @param keyword  名称关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SysPushInterface> findByInterfaceNameContaining(String keyword, Pageable pageable);
}
