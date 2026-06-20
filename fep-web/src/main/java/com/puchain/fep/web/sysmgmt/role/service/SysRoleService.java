package com.puchain.fep.web.sysmgmt.role.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.domain.PaginationHelper;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.rel.domain.SysRolePermission;
import com.puchain.fep.web.sysmgmt.rel.repository.SysRolePermissionRepository;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.RoleStatus;
import com.puchain.fep.web.sysmgmt.role.domain.RoleType;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.dto.RoleCreateRequest;
import com.puchain.fep.web.sysmgmt.role.dto.RolePermissionAssignRequest;
import com.puchain.fep.web.sysmgmt.role.dto.RoleResponse;
import com.puchain.fep.web.sysmgmt.role.dto.RoleUpdateRequest;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色管理服务。
 *
 * <p>提供角色 CRUD、状态切换、权限分配功能。
 * 参见 PRD v1.3 §5.10.2 角色管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysRoleService {

    private static final Logger log = LoggerFactory.getLogger(SysRoleService.class);

    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;

    /**
     * 构造 SysRoleService。
     *
     * @param roleRepository           角色 Repository
     * @param userRoleRepository        用户-角色关联 Repository
     * @param rolePermissionRepository  角色-权限关联 Repository
     */
    public SysRoleService(final SysRoleRepository roleRepository,
                          final SysUserRoleRepository userRoleRepository,
                          final SysRolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * 创建角色。
     *
     * @param request 创建请求
     * @return 新建角色响应
     * @throws FepBusinessException 角色编码已存在
     */
    @Transactional
    public RoleResponse create(final RoleCreateRequest request) {
        if (roleRepository.existsByRoleCode(request.getRoleCode())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "角色编码已存在: " + request.getRoleCode());
        }

        SysRole role = new SysRole();
        role.setRoleId(IdGenerator.uuid32());
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setRoleType(request.getRoleType());
        role.setDataScope(request.getDataScope());
        role.setRoleStatus(RoleStatus.ACTIVE);
        role.setRemark(request.getRemark());

        SysRole saved = roleRepository.save(role);
        log.info("Role created: code={}, type={}", saved.getRoleCode(), saved.getRoleType());
        return RoleResponse.from(saved);
    }

    /**
     * 更新角色。
     *
     * @param roleId  角色 ID
     * @param request 更新请求
     * @return 更新后的角色响应
     * @throws FepBusinessException 角色不存在
     */
    @Transactional
    public RoleResponse update(final String roleId, final RoleUpdateRequest request) {
        SysRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "角色不存在: " + roleId));

        role.setRoleName(request.getRoleName());
        role.setDataScope(request.getDataScope());
        role.setRemark(request.getRemark());

        SysRole saved = roleRepository.save(role);
        log.info("Role updated: code={}", saved.getRoleCode());
        return RoleResponse.from(saved);
    }

    /**
     * 删除角色（系统角色不可删除）。
     *
     * @param roleId 角色 ID
     * @throws FepBusinessException 角色不存在或为系统角色
     */
    @Transactional
    public void delete(final String roleId) {
        SysRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "角色不存在: " + roleId));

        if (role.getRoleType() == RoleType.SYSTEM) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "系统内置角色不允许删除: " + role.getRoleCode());
        }

        rolePermissionRepository.deleteByRoleId(roleId);
        userRoleRepository.deleteByRoleId(roleId);
        roleRepository.delete(role);
        log.info("Role deleted: code={}", role.getRoleCode());
    }

    /**
     * 切换角色状态（ACTIVE 与 DISABLED 互切）。
     *
     * @param roleId 角色 ID
     * @return 更新后的角色响应
     * @throws FepBusinessException 角色不存在
     */
    @Transactional
    public RoleResponse toggleStatus(final String roleId) {
        SysRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "角色不存在: " + roleId));

        if (role.getRoleStatus() == RoleStatus.ACTIVE) {
            role.setRoleStatus(RoleStatus.DISABLED);
        } else {
            role.setRoleStatus(RoleStatus.ACTIVE);
        }

        SysRole saved = roleRepository.save(role);
        log.info("Role status toggled: code={}, newStatus={}", saved.getRoleCode(), saved.getRoleStatus());
        return RoleResponse.from(saved);
    }

    /**
     * 按关键字搜索角色（分页）。
     *
     * @param keyword  角色名称关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<RoleResponse> search(final String keyword, final int pageNum, final int pageSize) {
        Pageable pageable = PaginationHelper.pageable(pageNum, pageSize, Sort.by("createTime").descending());

        Page<SysRole> page;
        if (keyword == null || keyword.isBlank()) {
            page = roleRepository.findAll(pageable);
        } else {
            page = roleRepository.findByRoleNameContaining(keyword, pageable);
        }

        return PageResult.from(page, pageNum, pageSize, RoleResponse::from);
    }

    /**
     * 分配角色权限（全量替换）。
     *
     * @param roleId  角色 ID
     * @param request 权限分配请求
     * @throws FepBusinessException 角色不存在
     */
    @Transactional
    public void assignPermissions(final String roleId,
                                  final RolePermissionAssignRequest request) {
        if (!roleRepository.existsById(roleId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "角色不存在: " + roleId);
        }

        // 删除旧权限
        rolePermissionRepository.deleteByRoleId(roleId);

        // 批量保存新权限
        List<SysRolePermission> newPerms = new ArrayList<>();
        if (request.getPermissions() != null) {
            for (RolePermissionAssignRequest.MenuPermission mp : request.getPermissions()) {
                for (String code : mp.getPermissionCodes()) {
                    newPerms.add(new SysRolePermission(roleId, mp.getMenuId(), code));
                }
            }
        }
        if (!newPerms.isEmpty()) {
            rolePermissionRepository.saveAll(newPerms);
        }

        log.info("Permissions assigned to role: roleId={}, count={}", roleId, newPerms.size());
    }

    /**
     * 获取角色已分配的权限列表。
     *
     * @param roleId 角色 ID
     * @return 权限关联列表
     * @throws FepBusinessException 角色不存在
     */
    public List<SysRolePermission> getPermissions(final String roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "角色不存在: " + roleId);
        }
        return rolePermissionRepository.findByRoleId(roleId);
    }
}
