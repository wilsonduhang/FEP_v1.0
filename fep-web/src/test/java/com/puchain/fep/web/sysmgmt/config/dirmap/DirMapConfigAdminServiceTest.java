package com.puchain.fep.web.sysmgmt.config.dirmap;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.DirMapConfigChangedEvent;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirMapConfigSnapshot;
import com.puchain.fep.processor.routing.DirMapConfigStore;
import com.puchain.fep.processor.routing.DirMapConfigUpdate;
import com.puchain.fep.processor.routing.ProcessingMode;
import com.puchain.fep.processor.routing.RoleDirection;
import com.puchain.fep.web.integration.dirmap.DirMapConfigHistoryEntity;
import com.puchain.fep.web.integration.dirmap.DirMapConfigHistoryRepository;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigResponse;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapHistoryResponse;
import com.puchain.fep.web.sysmgmt.config.dirmap.service.DirMapConfigAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DirMapConfigAdminService} — covers happy paths and
 * the 4 error branches the controller-level tests cannot reach.
 *
 * <p>v1i T5 quality reviewer P1-2 修复：补 Service 层单测，原有
 * {@code DirMapConfigControllerTest} 仅 mock service，Service 真实分支零覆盖
 * （JaCoCo 4% 行 / 0% 分支）。本类测：</p>
 * <ol>
 *   <li>{@code listAll()} 返回排序后列表（messageType, accessRole 双键）</li>
 *   <li>{@code update()} 成功 → 写 history → 调 store.update → publishEvent</li>
 *   <li>{@code update()} pre-check count != 88 → IllegalStateException（store.update 不被调）</li>
 *   <li>{@code update()} unknown messageType → IllegalArgumentException</li>
 *   <li>{@code update()} unknown accessRole → IllegalArgumentException</li>
 *   <li>{@code update()} 行不存在 → IllegalArgumentException</li>
 *   <li>{@code history()} 透传 repo 倒序结果</li>
 * </ol>
 *
 * <p>纯 Mockito 单测，无 Spring context — 与 sibling
 * {@code DynamicMessageDirectionMapTest} 同款模式。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DirMapConfigAdminServiceTest {

    @Mock
    private DirMapConfigStore configStore;

    @Mock
    private DirMapConfigHistoryRepository historyRepo;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DirMapConfigAdminService service;

    private static final long FULL_COUNT = 88L;

    @BeforeEach
    void setUp() {
        service = new DirMapConfigAdminService(configStore, historyRepo, eventPublisher);
    }

    @Test
    void listAll_returnsSortedByMessageTypeThenRole() {
        DirMapConfigSnapshot s3001Acc = new DirMapConfigSnapshot(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1,
                "system", Instant.parse("2026-04-29T00:00:00Z"));
        DirMapConfigSnapshot s3001Info = new DirMapConfigSnapshot(
                MessageType.MSG_3001, AccessRole.INFO_SERVICE_ORG,
                RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1,
                "system", Instant.parse("2026-04-29T00:00:00Z"));
        // findAll 顺序乱序，service 必须排序
        when(configStore.findAll()).thenReturn(List.of(s3001Info, s3001Acc));

        List<DirMapConfigResponse> result = service.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).messageType()).isEqualTo("3001");
        assertThat(result.get(0).accessRole()).isEqualTo("ACCEPTING_ORG");
        assertThat(result.get(1).accessRole()).isEqualTo("INFO_SERVICE_ORG");
    }

    @Test
    void update_writesHistory_callsStoreUpdate_publishesEvent() {
        DirMapConfigSnapshot before = new DirMapConfigSnapshot(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1,
                "system", Instant.parse("2026-04-29T00:00:00Z"));
        DirMapConfigSnapshot after = new DirMapConfigSnapshot(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3,
                "admin1", Instant.now());
        when(configStore.count()).thenReturn(FULL_COUNT);
        when(configStore.findOne(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG))
                .thenReturn(Optional.of(before));
        when(configStore.update(any(DirMapConfigUpdate.class))).thenReturn(after);

        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "OUTBOUND_ACTIVE", false, "MODE_3", "切换出向");
        DirMapConfigResponse result = service.update("3001", "ACCEPTING_ORG", req);

        // 1. history 行写入（before 状态记录）
        ArgumentCaptor<DirMapConfigHistoryEntity> historyCap =
                ArgumentCaptor.forClass(DirMapConfigHistoryEntity.class);
        verify(historyRepo).save(historyCap.capture());
        DirMapConfigHistoryEntity h = historyCap.getValue();
        assertThat(h.getMessageType()).isEqualTo("3001");
        assertThat(h.getAccessRole()).isEqualTo("ACCEPTING_ORG");
        assertThat(h.getOldDirection()).isEqualTo("INBOUND_PASSIVE");
        assertThat(h.getNewDirection()).isEqualTo("OUTBOUND_ACTIVE");
        assertThat(h.getOldRequiresFep()).isTrue();
        assertThat(h.getNewRequiresFep()).isFalse();
        assertThat(h.getChangeReason()).isEqualTo("切换出向");

        // 2. store.update 调用（6 args 契约 v1h P0-δ + v1i P0-D6）
        ArgumentCaptor<DirMapConfigUpdate> updateCap =
                ArgumentCaptor.forClass(DirMapConfigUpdate.class);
        verify(configStore).update(updateCap.capture());
        DirMapConfigUpdate upd = updateCap.getValue();
        assertThat(upd.messageType()).isEqualTo(MessageType.MSG_3001);
        assertThat(upd.accessRole()).isEqualTo(AccessRole.ACCEPTING_ORG);
        assertThat(upd.direction()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE);
        assertThat(upd.requiresFep()).isFalse();
        assertThat(upd.mode()).isEqualTo(ProcessingMode.MODE_3);

        // 3. event 发布（DirMapCacheInvalidator AFTER_COMMIT 监听）
        ArgumentCaptor<DirMapConfigChangedEvent> eventCap =
                ArgumentCaptor.forClass(DirMapConfigChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCap.capture());
        DirMapConfigChangedEvent ev = eventCap.getValue();
        assertThat(ev.getMsgType()).isEqualTo(MessageType.MSG_3001);
        assertThat(ev.getOldDirection()).isEqualTo(RoleDirection.INBOUND_PASSIVE);
        assertThat(ev.getNewDirection()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE);

        // 4. 返回值映射 after snapshot
        assertThat(result.direction()).isEqualTo("OUTBOUND_ACTIVE");
        assertThat(result.requiresFep()).isFalse();
    }

    @Test
    void update_throws_whenPreCountInvariantViolated() {
        when(configStore.count()).thenReturn(87L);
        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "OUTBOUND_ACTIVE", false, "MODE_3", "x");

        assertThatThrownBy(() -> service.update("3001", "ACCEPTING_ORG", req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DIR-MAP invariant violated")
                .hasMessageContaining("count=87")
                .hasMessageContaining("expected=88");

        // history / store.update / event 均不被调用 — pre-check 应在解析前抛
        verify(historyRepo, never()).save(any());
        verify(configStore, never()).update(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void update_throws_whenMessageTypeUnknown() {
        when(configStore.count()).thenReturn(FULL_COUNT);
        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "OUTBOUND_ACTIVE", false, "MODE_3", "x");

        assertThatThrownBy(() -> service.update("9999", "ACCEPTING_ORG", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown messageType: 9999");

        verify(historyRepo, never()).save(any());
        verify(configStore, never()).update(any());
    }

    @Test
    void update_throws_whenAccessRoleUnknown() {
        when(configStore.count()).thenReturn(FULL_COUNT);
        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "OUTBOUND_ACTIVE", false, "MODE_3", "x");

        // AccessRole.valueOf("BOGUS") throws IAE — Service 不显式 catch，
        // 由 GlobalExceptionHandler 映射 400
        assertThatThrownBy(() -> service.update("3001", "BOGUS_ROLE", req))
                .isInstanceOf(IllegalArgumentException.class);

        verify(historyRepo, never()).save(any());
    }

    @Test
    void update_throws_whenRowNotFoundInStore() {
        when(configStore.count()).thenReturn(FULL_COUNT);
        when(configStore.findOne(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG))
                .thenReturn(Optional.empty());
        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "OUTBOUND_ACTIVE", false, "MODE_3", "x");

        assertThatThrownBy(() -> service.update("3001", "ACCEPTING_ORG", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DIR-MAP config not found")
                .hasMessageContaining("3001/ACCEPTING_ORG");

        verify(historyRepo, never()).save(any());
        verify(configStore, never()).update(any());
    }

    @Test
    void history_returnsRepoResultsPreservingOrder() {
        DirMapConfigHistoryEntity older = new DirMapConfigHistoryEntity();
        older.setHistoryId("h1");
        older.setMessageType("3001");
        older.setAccessRole("ACCEPTING_ORG");
        older.setOldDirection("INBOUND_PASSIVE");
        older.setOldRequiresFep(true);
        older.setOldMode("MODE_1");
        older.setNewDirection("OUTBOUND_ACTIVE");
        older.setNewRequiresFep(false);
        older.setNewMode("MODE_3");
        older.setChangedBy("admin1");
        older.setChangedAt(Instant.parse("2026-04-29T00:00:00Z"));
        older.setChangeReason("初始切换");

        DirMapConfigHistoryEntity newer = new DirMapConfigHistoryEntity();
        newer.setHistoryId("h2");
        newer.setMessageType("3001");
        newer.setAccessRole("ACCEPTING_ORG");
        newer.setOldDirection("OUTBOUND_ACTIVE");
        newer.setOldRequiresFep(false);
        newer.setOldMode("MODE_3");
        newer.setNewDirection("INBOUND_PASSIVE");
        newer.setNewRequiresFep(true);
        newer.setNewMode("MODE_1");
        newer.setChangedBy("admin2");
        newer.setChangedAt(Instant.parse("2026-04-30T00:00:00Z"));
        newer.setChangeReason("回滚");

        // Repo derived query 返回倒序（newest first）
        when(historyRepo.findByMessageTypeAndAccessRoleOrderByChangedAtDesc(
                "3001", "ACCEPTING_ORG"))
                .thenReturn(List.of(newer, older));

        List<DirMapHistoryResponse> result = service.history("3001", "ACCEPTING_ORG");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).historyId()).isEqualTo("h2");
        assertThat(result.get(0).changeReason()).isEqualTo("回滚");
        assertThat(result.get(1).historyId()).isEqualTo("h1");
        verify(historyRepo, times(1))
                .findByMessageTypeAndAccessRoleOrderByChangedAtDesc("3001", "ACCEPTING_ORG");
    }
}
