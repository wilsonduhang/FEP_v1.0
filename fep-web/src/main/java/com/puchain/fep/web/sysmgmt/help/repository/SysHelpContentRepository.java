package com.puchain.fep.web.sysmgmt.help.repository;

import com.puchain.fep.web.sysmgmt.help.domain.SysHelpContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 帮助面板内容 Repository。
 *
 * <p>提供按页面编码和状态查询帮助内容的方法，结果按排序值升序排列。
 * 参见 PRD v1.3 §5.10.8 上下文帮助面板。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface SysHelpContentRepository extends JpaRepository<SysHelpContent, String> {

    /**
     * 按页面编码和帮助状态查询帮助内容，按排序值升序排列。
     *
     * @param pageCode   页面编码（如 sys-user、sys-role）
     * @param helpStatus 帮助状态（ACTIVE/DISABLED）
     * @return 符合条件的帮助内容列表，按 sortOrder 升序
     */
    List<SysHelpContent> findByPageCodeAndHelpStatusOrderBySortOrderAsc(
            String pageCode, String helpStatus);
}
