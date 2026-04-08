package com.puchain.fep.web.dashboard.shortcut.repository;

import com.puchain.fep.web.dashboard.shortcut.domain.DashboardShortcut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 首页快捷入口 Repository。
 *
 * <p>提供快捷入口的基本 CRUD 及按用户/可见性的查询。
 * 参见 PRD v1.3 §5.2.4 快捷入口（FR-WEB-DASH-QUICK）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface DashboardShortcutRepository extends JpaRepository<DashboardShortcut, String> {

    /**
     * 判断指定用户下是否存在同名快捷入口。
     *
     * @param userId       用户 ID
     * @param shortcutName 快捷入口名称
     * @return 存在则返回 true
     */
    boolean existsByUserIdAndShortcutName(String userId, String shortcutName);

    /**
     * 查询指定用户的可见快捷入口，按 sortOrder 升序排列。
     *
     * @param userId 用户 ID
     * @return 可见快捷入口列表
     */
    List<DashboardShortcut> findByUserIdAndVisibleTrueOrderBySortOrderAsc(String userId);

    /**
     * 查询指定用户的全部快捷入口（含隐藏），按 sortOrder 升序排列。
     *
     * @param userId 用户 ID
     * @return 全部快捷入口列表
     */
    List<DashboardShortcut> findByUserIdOrderBySortOrderAsc(String userId);
}
