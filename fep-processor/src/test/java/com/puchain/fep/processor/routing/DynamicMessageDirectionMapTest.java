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
}
