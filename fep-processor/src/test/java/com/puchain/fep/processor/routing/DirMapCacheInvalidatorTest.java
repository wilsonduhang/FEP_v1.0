package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.DirMapConfigChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * v1i T3 quality reviewer P1 修复：{@link DirMapCacheInvalidator#onChanged}
 * 此前无单测，事件 → reload 链路缺乏验证。本类用纯 Mockito 模拟事件触发并
 * 断言正确的 per-key reload 被调用。
 *
 * <p>T8 deferred drain #9（2026-05-01）：契约改为
 * {@link DynamicMessageDirectionMap#reload(DirMapKey)}（避免 88 行全表扫描）。
 * 测试同步更新到 per-key 断言。</p>
 *
 * <p>不需要 Spring context — invalidator 是 POJO，{@code @TransactionalEventListener}
 * 注解的实际事件分发由 sibling Spring container test 验证（T7 IT 兜底
 * commit-after-update 时序契约）。本类只验证 listener 方法体行为。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DirMapCacheInvalidatorTest {

    @Mock
    private DynamicMessageDirectionMap dynamicMap;

    @Test
    void onChanged_invokesPerKeyReloadExactlyOnce() {
        DirMapCacheInvalidator invalidator = new DirMapCacheInvalidator(dynamicMap);
        DirMapConfigChangedEvent event = new DirMapConfigChangedEvent(
                this,
                MessageType.MSG_3001,
                AccessRole.ACCEPTING_ORG,
                RoleDirection.INBOUND_PASSIVE,
                RoleDirection.OUTBOUND_ACTIVE,
                "admin1");

        invalidator.onChanged(event);

        verify(dynamicMap, times(1)).reload(
                new DirMapKey(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG));
        verifyNoMoreInteractions(dynamicMap);
    }
}
