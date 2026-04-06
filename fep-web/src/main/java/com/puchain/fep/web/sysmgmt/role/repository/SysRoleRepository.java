package com.puchain.fep.web.sysmgmt.role.repository;

import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 角色 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, String> {

    /**
     * 根据角色编码查找角色。
     *
     * @param roleCode 角色编码
     * @return 角色 Optional
     */
    Optional<SysRole> findByRoleCode(String roleCode);

    /**
     * 判断角色编码是否已存在。
     *
     * @param roleCode 角色编码
     * @return true 已存在
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * 按角色名称模糊搜索（分页）。
     *
     * @param keyword  角色名称关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SysRole> findByRoleNameContaining(String keyword, Pageable pageable);

    /**
     * 根据角色 ID 集合批量查找。
     *
     * @param roleIds 角色 ID 集合
     * @return 角色列表
     */
    List<SysRole> findByRoleIdIn(Collection<String> roleIds);
}
