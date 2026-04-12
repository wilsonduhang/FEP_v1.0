package com.puchain.fep.web.auth.service;

import com.puchain.fep.web.sysmgmt.rel.domain.SysRolePermission;
import com.puchain.fep.web.sysmgmt.rel.repository.SysRolePermissionRepository;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Aggregates permission codes for a given user by traversing
 * user -> roles -> permissions (2 SQL queries, deduped + sorted).
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class PermissionQueryHelper {

    private final SysUserRoleRepository userRoleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;

    /**
     * Constructs a PermissionQueryHelper.
     *
     * @param userRoleRepository       user-role association repository
     * @param rolePermissionRepository role-permission association repository
     */
    public PermissionQueryHelper(final SysUserRoleRepository userRoleRepository,
                                  final SysRolePermissionRepository rolePermissionRepository) {
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Returns a deduplicated, naturally-sorted, unmodifiable list of permission codes
     * for the specified user.
     *
     * @param userId user ID
     * @return permission codes; empty list if user has no roles or no permissions
     */
    public List<String> getPermissionCodes(final String userId) {
        final List<String> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(
                rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                        .map(SysRolePermission::getPermissionCode)
                        .collect(Collectors.toCollection(TreeSet::new))
                        .stream().toList()
        );
    }
}
