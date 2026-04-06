package com.puchain.fep.web.sysmgmt.role.service;

import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户角色查询辅助 — 从 userId 加载角色编码列表。
 *
 * <p>被 AuthService 和 SysUserService 共同使用，避免逻辑重复。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RoleQueryHelper {

    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;

    /**
     * 构造 RoleQueryHelper。
     *
     * @param userRoleRepository 用户-角色关联 Repository
     * @param roleRepository     角色 Repository
     */
    public RoleQueryHelper(final SysUserRoleRepository userRoleRepository,
                           final SysRoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * 查询用户的角色编码列表。
     *
     * @param userId 用户 ID
     * @return 角色编码列表（如 ["SYSTEM_ADMIN", "AUDITOR"]），无角色则空列表
     */
    public List<String> getRoleCodes(final String userId) {
        List<String> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.findByRoleIdIn(roleIds).stream()
                .map(SysRole::getRoleCode)
                .toList();
    }

    /**
     * 批量查询多个用户的角色编码列表（避免 N+1 查询问题）。
     *
     * <p>仅需 2 条 SQL：1 条查所有用户-角色关联，1 条查所有引用的角色。</p>
     *
     * @param userIds 用户 ID 列表
     * @return userId → 角色编码列表 的映射，无角色的用户不在 Map 中
     */
    public Map<String, List<String>> batchGetRoleCodes(final List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        // 1 query: get all user-role links
        List<SysUserRole> links = userRoleRepository.findByUserIdIn(userIds);
        if (links.isEmpty()) {
            return Map.of();
        }
        // 1 query: get all referenced roles
        List<String> roleIds = links.stream()
                .map(SysUserRole::getRoleId).distinct().toList();
        Map<String, String> roleIdToCode = roleRepository.findByRoleIdIn(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getRoleId, SysRole::getRoleCode));
        // Map userId → roleCodes
        return links.stream()
                .collect(Collectors.groupingBy(
                        SysUserRole::getUserId,
                        Collectors.mapping(
                                link -> roleIdToCode.getOrDefault(link.getRoleId(), ""),
                                Collectors.toList())));
    }
}
