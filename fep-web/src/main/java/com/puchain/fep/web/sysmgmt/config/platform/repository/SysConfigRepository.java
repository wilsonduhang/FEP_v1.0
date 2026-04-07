package com.puchain.fep.web.sysmgmt.config.platform.repository;

import com.puchain.fep.web.sysmgmt.config.platform.domain.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 通用系统配置 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface SysConfigRepository extends JpaRepository<SysConfig, String> {

    /**
     * 按配置分组查询所有配置项（按 sort_order 升序排序）。
     *
     * @param configGroup 配置分组（如 PLATFORM / SYSTEM / CERT）
     * @return 配置列表，按排序号升序
     */
    List<SysConfig> findByConfigGroupOrderBySortOrderAsc(String configGroup);

    /**
     * 按分组和键查询唯一配置。
     *
     * @param configGroup 配置分组
     * @param configKey   配置键
     * @return 配置项（可能为空）
     */
    Optional<SysConfig> findByConfigGroupAndConfigKey(String configGroup, String configKey);
}
