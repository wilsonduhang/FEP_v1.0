package com.puchain.fep.web.dashboard.shortcut.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.dashboard.shortcut.domain.DashboardShortcut;
import com.puchain.fep.web.dashboard.shortcut.dto.ShortcutCreateRequest;
import com.puchain.fep.web.dashboard.shortcut.dto.ShortcutResponse;
import com.puchain.fep.web.dashboard.shortcut.repository.DashboardShortcutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * DashboardShortcutService 单元测试。
 *
 * <p>覆盖创建、名称唯一性校验、可见列表排序、可见性切换等核心业务场景。</p>
 */
@ExtendWith(MockitoExtension.class)
class DashboardShortcutServiceTest {

    @Mock
    private DashboardShortcutRepository shortcutRepository;

    @InjectMocks
    private DashboardShortcutService shortcutService;

    @Test
    void create_withValidRequest_shouldReturnShortcutWithVisibleTrue() {
        ShortcutCreateRequest request = new ShortcutCreateRequest();
        request.setShortcutName("融资申请查询");
        request.setTargetUrl("/bizdata/records?type=3105");
        request.setIcon("search");

        when(shortcutRepository.existsByUserIdAndShortcutName(eq("user-001"), eq("融资申请查询")))
                .thenReturn(false);
        when(shortcutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShortcutResponse resp = shortcutService.create(request, "user-001");

        assertThat(resp.getShortcutName()).isEqualTo("融资申请查询");
        assertThat(resp.getTargetUrl()).isEqualTo("/bizdata/records?type=3105");
        assertThat(resp.getIcon()).isEqualTo("search");
        assertThat(resp.getVisible()).isTrue();
    }

    @Test
    void create_withDuplicateName_shouldThrowBiz5002() {
        ShortcutCreateRequest request = new ShortcutCreateRequest();
        request.setShortcutName("融资申请查询");
        request.setTargetUrl("/bizdata/records?type=3105");

        when(shortcutRepository.existsByUserIdAndShortcutName(eq("user-001"), eq("融资申请查询")))
                .thenReturn(true);

        assertThatThrownBy(() -> shortcutService.create(request, "user-001"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5002));
    }

    @Test
    void listVisible_shouldReturnOnlyVisibleSortedBySortOrder() {
        DashboardShortcut shortcutA = new DashboardShortcut();
        shortcutA.setShortcutId("s-001");
        shortcutA.setUserId("user-001");
        shortcutA.setShortcutName("A");
        shortcutA.setTargetUrl("/a");
        shortcutA.setSortOrder(2);
        shortcutA.setVisible(Boolean.TRUE);

        DashboardShortcut shortcutB = new DashboardShortcut();
        shortcutB.setShortcutId("s-002");
        shortcutB.setUserId("user-001");
        shortcutB.setShortcutName("B");
        shortcutB.setTargetUrl("/b");
        shortcutB.setSortOrder(1);
        shortcutB.setVisible(Boolean.TRUE);

        // Repository returns already sorted by sortOrder ASC: B(1), A(2)
        when(shortcutRepository.findByUserIdAndVisibleTrueOrderBySortOrderAsc("user-001"))
                .thenReturn(List.of(shortcutB, shortcutA));

        List<ShortcutResponse> result = shortcutService.listVisible("user-001");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getShortcutName()).isEqualTo("B");
        assertThat(result.get(0).getSortOrder()).isEqualTo(1);
        assertThat(result.get(1).getShortcutName()).isEqualTo("A");
        assertThat(result.get(1).getSortOrder()).isEqualTo(2);
    }

    @Test
    void toggleVisibility_shouldFlipVisibleFlag() {
        DashboardShortcut shortcut = new DashboardShortcut();
        shortcut.setShortcutId("s-001");
        shortcut.setUserId("user-001");
        shortcut.setShortcutName("测试快捷入口");
        shortcut.setTargetUrl("/test");
        shortcut.setSortOrder(0);
        shortcut.setVisible(Boolean.TRUE);

        when(shortcutRepository.findById("s-001")).thenReturn(Optional.of(shortcut));
        when(shortcutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShortcutResponse resp = shortcutService.toggleVisibility("s-001");

        assertThat(resp.getVisible()).isFalse();
    }
}
