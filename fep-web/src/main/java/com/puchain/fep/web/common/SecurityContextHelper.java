package com.puchain.fep.web.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security context utility for extracting current user info.
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {
        /* utility class */
    }

    /**
     * Get current authenticated user ID.
     *
     * @return user ID, or "anonymous" if not authenticated
     */
    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return "anonymous";
        }
        return auth.getName();
    }
}
