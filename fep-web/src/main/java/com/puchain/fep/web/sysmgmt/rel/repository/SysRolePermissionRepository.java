package com.puchain.fep.web.sysmgmt.rel.repository;

import com.puchain.fep.web.sysmgmt.rel.domain.SysRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 角色-权限关联 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysRolePermissionRepository extends JpaRepository<SysRolePermission, Long> {

    /**
     * 根据角色 ID 查找权限关联。
     *
     * @param roleId 角色 ID
     * @return 权限关联列表
     */
    List<SysRolePermission> findByRoleId(String roleId);

    /**
     * 根据角色 ID 集合批量查找权限关联。
     *
     * @param roleIds 角色 ID 集合
     * @return 权限关联列表
     */
    List<SysRolePermission> findByRoleIdIn(Collection<String> roleIds);

    /**
     * 根据角色 ID 删除所有权限关联。
     *
     * @param roleId 角色 ID
     */
    void deleteByRoleId(String roleId);
}
