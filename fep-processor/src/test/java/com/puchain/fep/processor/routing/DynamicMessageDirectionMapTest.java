package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.DirMapConfigChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.QueryTimeoutException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单元测试：mock {@link DirMapConfigStore} + {@link ApplicationEventPublisher}
 * 验证 {@link DynamicMessageDirectionMap} cache load / reload / lookupRaw / cacheSize
 * 行为契约。
 *
 * <p>每个 case 后清空 {@link MessageDirectionMapBridge} static 字段，避免
 * sibling test（如 {@code MessageDirectionMapFallbackTest}）被构造期 setDynamic
 * 副作用污染。</p>
 */
@ExtendWith(MockitoExtension.class)
class DynamicMessageDirectionMapTest {

    @Mock
    private DirMapConfigStore store;

    @Mock
    private ApplicationEventPublisher publisher;

    private DynamicMessageDirectionMap dynamicMap;

    private DirMapConfigSnapshot snapshot3001Acc;

    @BeforeEach
    void setUp() {
        snapshot3001Acc = new DirMapConfigSnapshot(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1,
                "system", Instant.parse("2026-04-29T00:00:00Z"));
        when(store.findAll()).thenReturn(List.of(snapshot3001Acc));
        dynamicMap = new DynamicMessageDirectionMap(store, publisher);
        dynamicMap.load();
    }

    @AfterEach
    void clearBridge() {
        MessageDirectionMapBridge.clearForTest();
    }

    @Test
    void load_populatesCacheFromStore_andReportsCacheSize() {
        assertThat(dynamicMap.cacheSize()).isEqualTo(1L);
    }

    @Test
    void lookupRaw_returnsCacheValue_whenCacheHit() {
        Optional<DirectionMapping> result = dynamicMap.lookupRaw(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(result).isPresent();
        DirectionMapping mapping = result.get();
        assertThat(mapping.direction()).isEqualTo(RoleDirection.INBOUND_PASSIVE);
        assertThat(mapping.mode()).isEqualTo(ProcessingMode.MODE_1);
        assertThat(mapping.requiresFep()).isTrue();
        // store.findOne never invoked when cache hits
        verify(store, times(0)).findOne(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
    }

    @Test
    void reload_replacesCacheAtomically_onSuccess() {
        DirMapConfigSnapshot updated = new DirMapConfigSnapshot(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3,
                "admin", Instant.now());
        when(store.findAll()).thenReturn(List.of(updated));

        dynamicMap.reload();

        Optional<DirectionMapping> result = dynamicMap.lookupRaw(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(result).isPresent();
        assertThat(result.get().direction()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE);
        assertThat(result.get().requiresFep()).isFalse();
    }

    @Test
    void reload_keepsPreviousCache_whenStoreThrows() {
        when(store.findAll()).thenThrow(new QueryTimeoutException("DB timeout"));

        dynamicMap.reload();

        // previous cache survives — still has 1 entry
        assertThat(dynamicMap.cacheSize()).isEqualTo(1L);
        Optional<DirectionMapping> result = dynamicMap.lookupRaw(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(result).isPresent();
        assertThat(result.get().direction()).isEqualTo(RoleDirection.INBOUND_PASSIVE);
    }

    @Test
    void publishReloadEvent_dispatchesViaPublisher() {
        DirMapConfigChangedEvent event = new DirMapConfigChangedEvent(
                this, MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                RoleDirection.INBOUND_PASSIVE, RoleDirection.OUTBOUND_ACTIVE, "admin");

        dynamicMap.publishReloadEvent(event);

        ArgumentCaptor<DirMapConfigChangedEvent> captor =
                ArgumentCaptor.forClass(DirMapConfigChangedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getMsgType()).isEqualTo(MessageType.MSG_3001);
        assertThat(captor.getValue().getNewDirection()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE);
    }

    /**
     * v1i T3 quality reviewer P1 修复：覆盖 L2 cache-miss 路径（cache 不命中 →
     * 调 {@code store.findOne} → 命中后填充 cache）。本类此前仅验证 cache-hit
     * 与 reload 路径，遗漏 lookupRaw 的核心 L2 分支。
     */
    @Test
    void lookupRaw_callsFindOne_andPopulatesCache_whenCacheMiss() {
        DirMapConfigSnapshot snapshot3002Acc = new DirMapConfigSnapshot(
                MessageType.MSG_3002, AccessRole.ACCEPTING_ORG,
                RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_1,
                "system", Instant.parse("2026-04-29T00:00:00Z"));
        // 3002/ACCEPTING_ORG 未在 setUp findAll 中加载 → cache miss → 走 findOne
        when(store.findOne(MessageType.MSG_3002, AccessRole.ACCEPTING_ORG))
                .thenReturn(Optional.of(snapshot3002Acc));

        long sizeBefore = dynamicMap.cacheSize();

        Optional<DirectionMapping> first = dynamicMap.lookupRaw(
                MessageType.MSG_3002, AccessRole.ACCEPTING_ORG);
        assertThat(first).isPresent();
        assertThat(first.get().direction()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE);
        verify(store, times(1)).findOne(MessageType.MSG_3002, AccessRole.ACCEPTING_ORG);

        // 二次调用必须从 cache 命中（findOne 不再被调用 → 仍只 1 次）
        Optional<DirectionMapping> second = dynamicMap.lookupRaw(
                MessageType.MSG_3002, AccessRole.ACCEPTING_ORG);
        assertThat(second).isPresent();
        verify(store, times(1)).findOne(MessageType.MSG_3002, AccessRole.ACCEPTING_ORG);

        assertThat(dynamicMap.cacheSize())
                .as("cache must grow by 1 after L2 hit fills it")
                .isEqualTo(sizeBefore + 1);
    }

    /**
     * v1i T3 quality reviewer P1 修复：L2 路径 DB 异常分支（{@code store.findOne}
     * 抛 {@link QueryTimeoutException}）必须被 catch、log.warn 并返回
     * {@link Optional#empty()}，而非传播异常。
     */
    @Test
    void lookupRaw_returnsEmpty_whenStoreThrowsDataAccessException() {
        when(store.findOne(MessageType.MSG_3002, AccessRole.ACCEPTING_ORG))
                .thenThrow(new QueryTimeoutException("DB timeout on findOne"));

        Optional<DirectionMapping> result = dynamicMap.lookupRaw(
                MessageType.MSG_3002, AccessRole.ACCEPTING_ORG);

        assertThat(result)
                .as("DB findOne 异常必须被 catch + 返回 empty，不阻断调用方")
                .isEmpty();
        verify(store, times(1)).findOne(MessageType.MSG_3002, AccessRole.ACCEPTING_ORG);
    }

    /**
     * T8 deferred drain #9（2026-05-01）：per-key reload 路径（事件驱动单条 PUT 失效）。
     * findOne 返回 present → cache 该键被覆盖为 fresh 值；不触发整表 findAll。
     */
    @Test
    void reloadKey_replacesSingleEntry_andLeavesOthersUntouched() {
        DirMapKey key3001 = new DirMapKey(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        DirMapConfigSnapshot fresh = new DirMapConfigSnapshot(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                RoleDirection.OUTBOUND_ACTIVE, false, ProcessingMode.MODE_3,
                "admin1", Instant.now());
        when(store.findOne(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG))
                .thenReturn(Optional.of(fresh));

        dynamicMap.reload(key3001);

        Optional<DirectionMapping> result = dynamicMap.lookupRaw(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(result).isPresent();
        assertThat(result.get().direction()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE);
        assertThat(result.get().mode()).isEqualTo(ProcessingMode.MODE_3);
        assertThat(result.get().requiresFep()).isFalse();
        // 整表 findAll 仅在 setUp 调一次（load），reload(key) 必须不再触发
        verify(store, times(1)).findAll();
        verify(store, times(1)).findOne(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
    }

    /**
     * findOne 返回 empty（行被删，理论上 D8 trigger 阻止但兜底） →
     * cache.invalidate(key) → 下次 lookupRaw 走 Port 二级查询。
     */
    @Test
    void reloadKey_invalidatesCache_whenStoreReturnsEmpty() {
        DirMapKey key3001 = new DirMapKey(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        when(store.findOne(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG))
                .thenReturn(Optional.empty());

        long sizeBefore = dynamicMap.cacheSize();
        dynamicMap.reload(key3001);

        assertThat(dynamicMap.cacheSize())
                .as("invalidate 后 cache 计数减 1")
                .isEqualTo(sizeBefore - 1);
    }

    /**
     * findOne 抛 DataAccessException → cache 该键保持原值（不清不替换）。
     */
    @Test
    void reloadKey_keepsCacheUnchanged_whenStoreThrowsDataAccessException() {
        DirMapKey key3001 = new DirMapKey(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        when(store.findOne(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG))
                .thenThrow(new QueryTimeoutException("DB timeout on per-key reload"));

        dynamicMap.reload(key3001);

        Optional<DirectionMapping> result = dynamicMap.lookupRaw(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(result)
                .as("DB 异常时 cache 保持启动期 INBOUND_PASSIVE，不被覆盖也不被清空")
                .isPresent();
        assertThat(result.get().direction()).isEqualTo(RoleDirection.INBOUND_PASSIVE);
    }
}
