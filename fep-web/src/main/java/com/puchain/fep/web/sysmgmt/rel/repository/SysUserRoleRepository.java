package com.puchain.fep.web.sysmgmt.rel.repository;

import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户-角色关联 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {

    /**
     * 根据用户 ID 查找关联。
     *
     * @param userId 用户 ID
     * @return 关联列表
     */
    List<SysUserRole> findByUserId(String userId);

    /**
     * 根据角色 ID 查找关联。
     *
     * @param roleId 角色 ID
     * @return 关联列表
     */
    List<SysUserRole> findByRoleId(String roleId);

    /**
     * 根据用户 ID 删除所有关联。
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(String userId);

    /**
     * 根据角色 ID 删除所有关联。
     *
     * @param roleId 角色 ID
     */
    void deleteByRoleId(String roleId);

    /**
     * 查询用户关联的角色 ID 列表。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    @Query("SELECT ur.roleId FROM SysUserRole ur WHERE ur.userId = :userId")
    List<String> findRoleIdsByUserId(@Param("userId") String userId);

    /**
     * 根据用户 ID 集合批量查找关联。
     *
     * @param userIds 用户 ID 集合
     * @return 关联列表
     */
    List<SysUserRole> findByUserIdIn(List<String> userIds);
}
