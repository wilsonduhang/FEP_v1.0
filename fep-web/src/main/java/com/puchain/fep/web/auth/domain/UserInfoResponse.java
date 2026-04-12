package com.puchain.fep.web.auth.domain;

import com.puchain.fep.web.sysmgmt.menu.dto.MenuTreeNode;

import java.util.List;

/**
 * Response DTO for {@code GET /api/v1/auth/me}.
 *
 * <p>Aggregates the current user's profile, role codes, permission codes,
 * and accessible menu tree for front-end permission guards.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class UserInfoResponse {

    private final String userId;
    private final String userAccount;
    private final String userName;
    private final String phone;
    private final String email;
    private final String department;
    private final List<String> roleCodes;
    private final List<String> permissions;
    private final List<MenuTreeNode> menuTree;

    /**
     * Constructs a UserInfoResponse.
     *
     * @param userId      user ID
     * @param userAccount user login account
     * @param userName    user display name
     * @param phone       phone number (nullable)
     * @param email       email address (nullable)
     * @param department  department name (nullable)
     * @param roleCodes   role code list
     * @param permissions deduplicated, sorted permission codes
     * @param menuTree    accessible menu tree nodes
     */
    public UserInfoResponse(final String userId, final String userAccount,
                            final String userName, final String phone,
                            final String email, final String department,
                            final List<String> roleCodes,
                            final List<String> permissions,
                            final List<MenuTreeNode> menuTree) {
        this.userId = userId;
        this.userAccount = userAccount;
        this.userName = userName;
        this.phone = phone;
        this.email = email;
        this.department = department;
        this.roleCodes = roleCodes;
        this.permissions = permissions;
        this.menuTree = menuTree;
    }

    /** Returns the user ID. */
    public String getUserId() {
        return userId;
    }

    /** Returns the user login account. */
    public String getUserAccount() {
        return userAccount;
    }

    /** Returns the user display name. */
    public String getUserName() {
        return userName;
    }

    /** Returns the phone number (may be null). */
    public String getPhone() {
        return phone;
    }

    /** Returns the email address (may be null). */
    public String getEmail() {
        return email;
    }

    /** Returns the department name (may be null). */
    public String getDepartment() {
        return department;
    }

    /** Returns the role code list. */
    public List<String> getRoleCodes() {
        return roleCodes;
    }

    /** Returns the deduplicated, sorted permission codes. */
    public List<String> getPermissions() {
        return permissions;
    }

    /** Returns the accessible menu tree nodes. */
    public List<MenuTreeNode> getMenuTree() {
        return menuTree;
    }
}
