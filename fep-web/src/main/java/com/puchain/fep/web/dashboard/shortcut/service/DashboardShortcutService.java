package com.puchain.fep.web.dashboard.shortcut.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.dashboard.shortcut.domain.DashboardShortcut;
import com.puchain.fep.web.dashboard.shortcut.dto.ShortcutCreateRequest;
import com.puchain.fep.web.dashboard.shortcut.dto.ShortcutReorderRequest;
import com.puchain.fep.web.dashboard.shortcut.dto.ShortcutResponse;
import com.puchain.fep.web.dashboard.shortcut.repository.DashboardShortcutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 首页快捷入口管理 Service。
 *
 * <p>提供快捷入口 CRUD、排序管理及可见性切换功能。
 * 参见 PRD v1.3 §5.2.4 快捷入口（FR-WEB-DASH-QUICK）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class DashboardShortcutService {

    private static final Logger log = LoggerFactory.getLogger(DashboardShortcutService.class);

    private final DashboardShortcutRepository shortcutRepository;

    /**
     * 构造 DashboardShortcutService。
     *
     * @param shortcutRepository 快捷入口 Repository
     */
    public DashboardShortcutService(final DashboardShortcutRepository shortcutRepository) {
        this.shortcutRepository = shortcutRepository;
    }

    /**
     * 创建快捷入口。
     *
     * <p>同一用户下快捷入口名称不能重复，否则抛出 BIZ_5002。</p>
     *
     * @param request 创建请求
     * @param userId  用户 ID
     * @return 快捷入口响应
     * @throws FepBusinessException 名称重复（BIZ_5002）
     */
    @Transactional
    public ShortcutResponse create(final ShortcutCreateRequest request, final String userId) {
        if (shortcutRepository.existsByUserIdAndShortcutName(userId, request.getShortcutName())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "快捷入口名称已存在: " + LogSanitizer.sanitize(request.getShortcutName()));
        }

        DashboardShortcut entity = new DashboardShortcut();
        entity.setShortcutId(IdGenerator.uuid32());
        entity.setUserId(userId);
        entity.setShortcutName(request.getShortcutName());
        entity.setTargetUrl(request.getTargetUrl());
        entity.setIcon(request.getIcon());
        entity.setSortOrder(request.getSortOrder());
        entity.setVisible(Boolean.TRUE);

        DashboardShortcut saved = shortcutRepository.save(entity);
        log.info("Created dashboard shortcut: id={}, name={}",
                saved.getShortcutId(), LogSanitizer.sanitize(saved.getShortcutName()));
        return ShortcutResponse.from(saved);
    }

    /**
     * 查询当前用户的可见快捷入口列表，按 sortOrder 升序排列。
     *
     * @param userId 用户 ID
     * @return 可见快捷入口列表
     */
    @Transactional(readOnly = true)
    public List<ShortcutResponse> listVisible(final String userId) {
        return shortcutRepository.findByUserIdAndVisibleTrueOrderBySortOrderAsc(userId)
                .stream()
                .map(ShortcutResponse::from)
                .toList();
    }

    /**
     * 查询当前用户的全部快捷入口（含隐藏），按 sortOrder 升序排列。
     *
     * @param userId 用户 ID
     * @return 全部快捷入口列表
     */
    @Transactional(readOnly = true)
    public List<ShortcutResponse> listAll(final String userId) {
        return shortcutRepository.findByUserIdOrderBySortOrderAsc(userId)
                .stream()
                .map(ShortcutResponse::from)
                .toList();
    }

    /**
     * 切换快捷入口的可见性（visible 取反）。
     *
     * @param shortcutId 快捷入口 ID
     * @return 更新后的快捷入口响应
     * @throws FepBusinessException 快捷入口不存在（BIZ_5001）
     */
    @Transactional
    public ShortcutResponse toggleVisibility(final String shortcutId) {
        DashboardShortcut entity = shortcutRepository.findById(shortcutId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "快捷入口不存在: " + shortcutId));

        entity.setVisible(!entity.getVisible());

        DashboardShortcut saved = shortcutRepository.save(entity);
        log.info("Toggled shortcut visibility: id={}, visible={}",
                saved.getShortcutId(), saved.getVisible());
        return ShortcutResponse.from(saved);
    }

    /**
     * 批量更新快捷入口排序。
     *
     * @param request 重排序请求
     */
    @Transactional
    public void reorder(final ShortcutReorderRequest request) {
        for (ShortcutReorderRequest.ReorderItem item : request.getItems()) {
            DashboardShortcut entity = shortcutRepository.findById(item.getShortcutId())
                    .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                            "快捷入口不存在: " + item.getShortcutId()));
            entity.setSortOrder(item.getSortOrder());
            shortcutRepository.save(entity);
        }
        log.info("Reordered {} dashboard shortcuts", request.getItems().size());
    }

    /**
     * 删除快捷入口。
     *
     * @param shortcutId 快捷入口 ID
     * @throws FepBusinessException 快捷入口不存在（BIZ_5001）
     */
    @Transactional
    public void delete(final String shortcutId) {
        if (!shortcutRepository.existsById(shortcutId)) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                    "快捷入口不存在: " + shortcutId);
        }
        shortcutRepository.deleteById(shortcutId);
        log.info("Deleted dashboard shortcut: id={}", shortcutId);
    }
}
