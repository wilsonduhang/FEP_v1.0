package com.puchain.fep.web.sysmgmt.user.repository;

import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, String> {

    /**
     * 根据登录账号查找用户。
     *
     * @param userAccount 登录账号
     * @return 用户 Optional
     */
    Optional<SysUser> findByUserAccount(String userAccount);

    /**
     * 判断登录账号是否已存在。
     *
     * @param userAccount 登录账号
     * @return true 已存在
     */
    boolean existsByUserAccount(String userAccount);

    /**
     * 按用户状态分页查询。
     *
     * @param status   用户状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SysUser> findByUserStatus(UserStatus status, Pageable pageable);

    /**
     * 按用户名或账号模糊搜索（分页）。
     *
     * @param nameKeyword    用户名关键字
     * @param accountKeyword 账号关键字
     * @param pageable       分页参数
     * @return 分页结果
     */
    Page<SysUser> findByUserNameContainingOrUserAccountContaining(
            String nameKeyword, String accountKeyword, Pageable pageable);
}
