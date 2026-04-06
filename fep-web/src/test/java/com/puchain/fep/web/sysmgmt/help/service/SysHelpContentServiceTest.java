package com.puchain.fep.web.sysmgmt.help.service;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.help.dto.HelpContentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SysHelpContentService 集成测试。
 *
 * <p>使用 H2 内存数据库（dev profile）+ Flyway 迁移验证帮助面板服务全流程：
 * 查询、新增、局部更新（含 null 字段不更新的边界情况）。
 * 参见 PRD v1.3 §5.10.8 上下文帮助面板。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
class SysHelpContentServiceTest {

    @Autowired
    private SysHelpContentService helpService;

    /**
     * 测试1: 查询已存在的页面编码应返回 sys-user 种子数据（2 条）。
     */
    @Test
    void findByPageCode_seedData_shouldReturn2Items() {
        List<HelpContentResponse> result = helpService.findByPageCode("sys-user");

        assertEquals(2, result.size(), "sys-user 页面应有 2 条种子帮助数据");
        assertEquals("如何创建用户", result.get(0).getTitle(), "第一条应按 sortOrder 排序");
        assertEquals("sys-user", result.get(0).getPageCode());
    }

    /**
     * 测试2: 查询不存在的页面编码应返回空列表。
     */
    @Test
    void findByPageCode_nonexistent_shouldReturnEmpty() {
        List<HelpContentResponse> result = helpService.findByPageCode("nonexistent-page-xyz");

        assertNotNull(result, "结果不应为 null");
        assertEquals(0, result.size(), "不存在的页面编码应返回空列表");
    }

    /**
     * 测试3: 新增帮助内容应持久化并返回正确字段。
     */
    @Test
    void create_shouldPersistAndReturnResponse() {
        HelpContentResponse resp = helpService.create(
                "test-svc-page", "服务层测试标题", "服务层测试摘要", "服务层测试内容");

        assertNotNull(resp.getHelpId(), "helpId 应自动生成");
        assertEquals("test-svc-page", resp.getPageCode());
        assertEquals("服务层测试标题", resp.getTitle());
        assertEquals("服务层测试摘要", resp.getSummary());
        assertEquals("服务层测试内容", resp.getContent());
        assertEquals(1, resp.getSortOrder(), "首条记录 sortOrder 应为 1");
    }

    /**
     * 测试4a: 更新已存在条目时仅更新非 null 字段（标题更新，摘要/内容 null 不更新）。
     */
    @Test
    void update_partialUpdate_titleOnly_shouldOnlyUpdateTitle() {
        // 先创建一条记录
        HelpContentResponse created = helpService.create(
                "test-update-page", "原标题", "原摘要", "原内容");

        // 只更新标题，摘要和内容传 null
        HelpContentResponse updated = helpService.update(
                created.getHelpId(), "新标题", null, null);

        assertEquals("新标题", updated.getTitle(), "标题应已更新");
        assertEquals("原摘要", updated.getSummary(), "摘要不应改变（null 不更新）");
        assertEquals("原内容", updated.getContent(), "内容不应改变（null 不更新）");
    }

    /**
     * 测试4b: 全字段更新——标题/摘要/内容均非 null 时全部更新。
     */
    @Test
    void update_allFieldsNonNull_shouldUpdateAllFields() {
        HelpContentResponse created = helpService.create(
                "test-update-page2", "原标题", "原摘要", "原内容");

        // 全部字段均传非 null，覆盖 summary != null 和 content != null 的 true 分支
        HelpContentResponse updated = helpService.update(
                created.getHelpId(), "新标题", "新摘要", "新内容");

        assertEquals("新标题", updated.getTitle(), "标题应已更新");
        assertEquals("新摘要", updated.getSummary(), "摘要应已更新");
        assertEquals("新内容", updated.getContent(), "内容应已更新");
    }

    /**
     * 测试4c: 标题传 null 时仅更新摘要和内容，标题保持原值（覆盖 title == null 分支）。
     */
    @Test
    void update_titleNull_shouldOnlyUpdateSummaryAndContent() {
        HelpContentResponse created = helpService.create(
                "test-update-page3", "原标题", "原摘要", "原内容");

        // 标题传 null，覆盖 title == null 的 false 分支
        HelpContentResponse updated = helpService.update(
                created.getHelpId(), null, "新摘要", "新内容");

        assertEquals("原标题", updated.getTitle(), "标题不应改变（null 不更新）");
        assertEquals("新摘要", updated.getSummary(), "摘要应已更新");
        assertEquals("新内容", updated.getContent(), "内容应已更新");
    }

    /**
     * 测试5: 更新不存在的 helpId 应抛出 BIZ_5001 业务异常。
     */
    @Test
    void update_notFound_shouldThrowBusinessException() {
        assertThrows(FepBusinessException.class,
                () -> helpService.update("nonexistent-id", "新标题", null, null),
                "不存在的 helpId 应抛出业务异常");
    }

    /**
     * 测试6: 超过 4 条时 findByPageCode 应最多返回 4 条（PRD §5.10.8 限制）。
     */
    @Test
    void findByPageCode_moreThan4Items_shouldReturnMax4() {
        String pageCode = "test-max-page";
        for (int i = 1; i <= 5; i++) {
            helpService.create(pageCode, "标题" + i, "摘要" + i, "内容" + i);
        }

        List<HelpContentResponse> result = helpService.findByPageCode(pageCode);

        assertEquals(4, result.size(), "findByPageCode 最多应返回 4 条");
    }
}
