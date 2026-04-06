package com.puchain.fep.web.sysmgmt.help.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.sysmgmt.help.domain.SysHelpContent;
import com.puchain.fep.web.sysmgmt.help.dto.HelpContentResponse;
import com.puchain.fep.web.sysmgmt.help.repository.SysHelpContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帮助面板内容服务。
 *
 * <p>提供按页面编码查询帮助内容（最多 4 条）、新增帮助条目、局部更新帮助条目功能。
 * 参见 PRD v1.3 §5.10.8 上下文帮助面板。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysHelpContentService {

    private static final Logger log = LoggerFactory.getLogger(SysHelpContentService.class);

    /** PRD §5.10.8 规定每页最多展示 4 条帮助内容。 */
    private static final int MAX_HELP_ITEMS = 4;

    /** 帮助状态：启用。 */
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final SysHelpContentRepository helpRepository;

    /**
     * 构造 SysHelpContentService。
     *
     * @param helpRepository 帮助内容 Repository
     */
    public SysHelpContentService(final SysHelpContentRepository helpRepository) {
        this.helpRepository = helpRepository;
    }

    /**
     * 按页面编码查询启用状态的帮助内容，最多返回 4 条（PRD §5.10.8）。
     *
     * @param pageCode 页面编码（如 sys-user、sys-role）
     * @return 帮助内容响应 DTO 列表（最多 4 条，按 sortOrder 升序）
     */
    public List<HelpContentResponse> findByPageCode(final String pageCode) {
        List<SysHelpContent> all = helpRepository
                .findByPageCodeAndHelpStatusOrderBySortOrderAsc(pageCode, STATUS_ACTIVE);
        return all.stream()
                .limit(MAX_HELP_ITEMS)
                .map(HelpContentResponse::from)
                .toList();
    }

    /**
     * 新增帮助内容条目。
     *
     * <p>新建条目的 sortOrder 默认为当前该页面已有条目数量 + 1，helpStatus 默认为 ACTIVE。</p>
     *
     * @param pageCode 页面编码
     * @param title    帮助标题
     * @param summary  简要描述
     * @param content  详细内容
     * @return 已保存的帮助内容响应 DTO
     */
    @Transactional
    public HelpContentResponse create(final String pageCode,
                                      final String title,
                                      final String summary,
                                      final String content) {
        List<SysHelpContent> existing = helpRepository
                .findByPageCodeAndHelpStatusOrderBySortOrderAsc(pageCode, STATUS_ACTIVE);
        int nextSortOrder = existing.size() + 1;

        SysHelpContent entity = new SysHelpContent();
        entity.setHelpId(IdGenerator.uuid32());
        entity.setPageCode(pageCode);
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setContent(content);
        entity.setSortOrder(nextSortOrder);
        entity.setHelpStatus(STATUS_ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        SysHelpContent saved = helpRepository.save(entity);
        log.info("HelpContent created: helpId={}, pageCode={}", saved.getHelpId(), saved.getPageCode());
        return HelpContentResponse.from(saved);
    }

    /**
     * 局部更新帮助内容条目（仅更新非 null 字段）。
     *
     * <p>帮助条目不存在时抛出 BIZ_5001 业务异常。</p>
     *
     * @param helpId  帮助 ID
     * @param title   新标题，传 null 表示不更新
     * @param summary 新简要描述，传 null 表示不更新
     * @param content 新详细内容，传 null 表示不更新
     * @return 更新后的帮助内容响应 DTO
     * @throws FepBusinessException 帮助条目不存在时
     */
    @Transactional
    public HelpContentResponse update(final String helpId,
                                      final String title,
                                      final String summary,
                                      final String content) {
        SysHelpContent entity = helpRepository.findById(helpId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "帮助内容不存在: " + helpId));

        if (title != null) {
            entity.setTitle(title);
        }
        if (summary != null) {
            entity.setSummary(summary);
        }
        if (content != null) {
            entity.setContent(content);
        }
        entity.setUpdateTime(LocalDateTime.now());

        SysHelpContent saved = helpRepository.save(entity);
        log.info("HelpContent updated: helpId={}", saved.getHelpId());
        return HelpContentResponse.from(saved);
    }
}
