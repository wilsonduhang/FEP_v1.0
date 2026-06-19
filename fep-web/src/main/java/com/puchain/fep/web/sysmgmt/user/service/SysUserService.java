package com.puchain.fep.web.sysmgmt.user.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.domain.PaginationHelper;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.security.PasswordHasher;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.service.RoleQueryHelper;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.domain.UserStatus;
import com.puchain.fep.web.sysmgmt.user.dto.ResetPasswordRequest;
import com.puchain.fep.web.sysmgmt.user.dto.UserCreateRequest;
import com.puchain.fep.web.sysmgmt.user.dto.UserResponse;
import com.puchain.fep.web.sysmgmt.user.dto.UserUpdateRequest;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 用户管理服务。
 *
 * <p>提供用户 CRUD、状态切换、密码重置、关键字搜索功能。
 * 参见 PRD v1.3 §5.10.1 用户管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysUserService {

    private static final Logger log = LoggerFactory.getLogger(SysUserService.class);

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final RoleQueryHelper roleQueryHelper;
    private final PasswordHasher passwordHasher;

    /**
     * 构造 SysUserService。
     *
     * @param userRepository     用户 Repository
     * @param userRoleRepository 用户-角色关联 Repository
     * @param roleQueryHelper    角色查询辅助
     * @param passwordHasher     密码散列器
     */
    public SysUserService(final SysUserRepository userRepository,
                          final SysUserRoleRepository userRoleRepository,
                          final RoleQueryHelper roleQueryHelper,
                          final PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleQueryHelper = roleQueryHelper;
        this.passwordHasher = passwordHasher;
    }

    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @return 用户响应（含角色编码）
     * @throws FepBusinessException 账号已存在
     */
    @Transactional
    public UserResponse create(final UserCreateRequest request) {
        if (userRepository.existsByUserAccount(request.getAccount())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "登录账号已存在: " + request.getAccount());
        }

        SysUser user = new SysUser();
        user.setUserId(IdGenerator.uuid32());
        user.setUserAccount(request.getAccount());
        user.setUserName(request.getUserName());
        user.setPasswordHash(passwordHasher.hash(request.getInitialPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setUserStatus(UserStatus.ACTIVE);
        user.setLoginFailCount(0);

        SysUser saved = userRepository.save(user);

        // 分配角色
        List<String> roleCodes = assignRoles(saved.getUserId(), request.getRoleIds());

        log.info("User created: account={}", saved.getUserAccount());
        return UserResponse.from(saved, roleCodes);
    }

    /**
     * 更新用户信息。
     *
     * @param userId  用户 ID
     * @param request 更新请求
     * @return 更新后的用户响应
     * @throws FepBusinessException 用户不存在
     */
    @Transactional
    public UserResponse update(final String userId, final UserUpdateRequest request) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "用户不存在: " + userId));

        if (request.getUserName() != null) {
            user.setUserName(request.getUserName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }

        SysUser saved = userRepository.save(user);

        // 重新分配角色（如果提供）
        List<String> roleCodes;
        if (request.getRoleIds() != null) {
            roleCodes = assignRoles(userId, request.getRoleIds());
        } else {
            roleCodes = roleQueryHelper.getRoleCodes(userId);
        }

        log.info("User updated: account={}", saved.getUserAccount());
        return UserResponse.from(saved, roleCodes);
    }

    /**
     * 删除用户。
     *
     * @param userId 用户 ID
     * @throws FepBusinessException 用户不存在
     */
    @Transactional
    public void delete(final String userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "用户不存在: " + userId));

        userRoleRepository.deleteByUserId(userId);
        userRepository.delete(user);
        log.info("User deleted: account={}", user.getUserAccount());
    }

    /**
     * 设置用户状态（启用/禁用）。
     *
     * <p>启用时清除锁定时间和失败计数。</p>
     *
     * @param userId 用户 ID
     * @param status 目标状态
     * @return 更新后的用户响应
     * @throws FepBusinessException 用户不存在
     */
    @Transactional
    public UserResponse setStatus(final String userId, final UserStatus status) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "用户不存在: " + userId));

        user.setUserStatus(status);
        if (status == UserStatus.ACTIVE) {
            user.setLockedUntil(null);
            user.setLoginFailCount(0);
        }

        SysUser saved = userRepository.save(user);
        List<String> roleCodes = roleQueryHelper.getRoleCodes(userId);

        log.info("User status changed: account={}, status={}", saved.getUserAccount(), status);
        return UserResponse.from(saved, roleCodes);
    }

    /**
     * 重置密码。
     *
     * <p>设置 lastPasswordChangeTime 为 null，强制下次登录时修改密码。</p>
     *
     * @param userId  用户 ID
     * @param request 重置密码请求
     * @throws FepBusinessException 用户不存在
     */
    @Transactional
    public void resetPassword(final String userId, final ResetPasswordRequest request) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "用户不存在: " + userId));

        user.setPasswordHash(passwordHasher.hash(request.getNewPassword()));
        user.setLastPasswordChangeTime(null);

        userRepository.save(user);
        log.info("User password reset: account={}", user.getUserAccount());
    }

    /**
     * 按关键字搜索用户（分页）。
     *
     * <p>关键字同时匹配用户名和登录账号。</p>
     *
     * @param keyword  关键字（可为空/null 表示全量查询）
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    public PageResult<UserResponse> search(final String keyword, final int pageNum, final int pageSize) {
        Pageable pageable = PaginationHelper.pageable(pageNum, pageSize, Sort.by("createTime").descending());

        Page<SysUser> page;
        if (keyword == null || keyword.isBlank()) {
            page = userRepository.findAll(pageable);
        } else {
            page = userRepository.findByUserNameContainingOrUserAccountContaining(
                    keyword, keyword, pageable);
        }

        // Batch load roles for all users on this page (instead of N+1)
        List<String> userIds = page.getContent().stream()
                .map(SysUser::getUserId).toList();
        Map<String, List<String>> roleCodesByUserId = roleQueryHelper.batchGetRoleCodes(userIds);

        return PageResult.from(page, pageNum, pageSize,
                u -> UserResponse.from(u,
                        roleCodesByUserId.getOrDefault(u.getUserId(), List.of())));
    }

    /**
     * 根据 ID 查找用户。
     *
     * @param userId 用户 ID
     * @return 用户响应
     * @throws FepBusinessException 用户不存在
     */
    public UserResponse findById(final String userId) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "用户不存在: " + userId));
        List<String> roleCodes = roleQueryHelper.getRoleCodes(userId);
        return UserResponse.from(user, roleCodes);
    }

    /**
     * 分配角色（全量替换）并返回角色编码列表。
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表（可为 null）
     * @return 角色编码列表
     */
    private List<String> assignRoles(final String userId, final List<String> roleIds) {
        userRoleRepository.deleteByUserId(userId);

        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        for (String roleId : roleIds) {
            userRoleRepository.save(new SysUserRole(userId, roleId));
        }

        return roleQueryHelper.getRoleCodes(userId);
    }
}
