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
 * 此前无单测，事件 → reload() 链路缺乏验证。本类用纯 Mockito 模拟事件触发
 * 并断言 {@link DynamicMessageDirectionMap#reload()} 被调用恰好一次。
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
    void onChanged_invokesReloadExactlyOnce() {
        DirMapCacheInvalidator invalidator = new DirMapCacheInvalidator(dynamicMap);
        DirMapConfigChangedEvent event = new DirMapConfigChangedEvent(
                this,
                MessageType.MSG_3001,
                AccessRole.ACCEPTING_ORG,
                RoleDirection.INBOUND_PASSIVE,
                RoleDirection.OUTBOUND_ACTIVE,
                "admin1");

        invalidator.onChanged(event);

        verify(dynamicMap, times(1)).reload();
        verifyNoMoreInteractions(dynamicMap);
    }
}
