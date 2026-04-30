package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * v1i P0-C6 修复（Round 6 K3 抓出）：fep-processor 是 lib 模块<b>无</b>
 * {@code @SpringBootApplication}，{@code @SpringBootTest} 向上扫包找不到主配置类启动失败。
 *
 * <p>cold-start 测试本质就是验证"new {@link DynamicMessageDirectionMap} + Bridge static 字段"
 * 的纯逻辑路径，不依赖 Spring ctx —— v1h P0-ζ 用 {@code @SpringBootTest} 是过度设计。
 * 本类改纯 Mockito + JUnit 5 单测：</p>
 * <ul>
 *   <li>{@code @ExtendWith(MockitoExtension.class)} 替代 {@code @SpringBootTest}</li>
 *   <li>{@code @Mock DirMapConfigStore} 替代 {@code @MockBean}</li>
 *   <li>无 ctx → 启动 ~50ms 而非 ~10s</li>
 * </ul>
 *
 * <p>类名从 {@code *IT} 改为 {@code *Test} —— surefire {@code *Test.java} 模式自动收集，
 * IT 命名仅 failsafe 收集（fep-processor 项目惯例）。</p>
 */
@ExtendWith(MockitoExtension.class)
class DynamicMessageDirectionMapColdStartTest {

    @Mock
    private DirMapConfigStore store;

    @Test
    void coldStart_emptyCache_lookupDelegatesToStatic() {
        when(store.findAll()).thenThrow(new QueryTimeoutException("DB down"));
        when(store.findOne(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG))
                .thenThrow(new QueryTimeoutException("findOne also down"));

        // 构造 cold-start 实例（构造函数 P0-A 自动 setDynamic → Bridge 指向 cold）。
        DynamicMessageDirectionMap cold =
                new DynamicMessageDirectionMap(store, event -> {
                    // no-op publisher for cold-start verification
                });
        cold.load();

        assertThat(cold.cacheSize())
                .as("cache must be empty when store.findAll throws on cold start")
                .isZero();

        // v1g P0-D — 走 MessageDirectionMap.lookup 公共 API：Bridge → cold.lookupRaw
        // (empty) → staticLookup → TABLE 命中。3001 ACCEPTING_ORG → INBOUND_PASSIVE。
        Optional<DirectionMapping> result = MessageDirectionMap.lookup(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);

        assertThat(result).isPresent();
        assertThat(result.get().direction()).isEqualTo(RoleDirection.INBOUND_PASSIVE);
        assertThat(result.get().mode()).isEqualTo(ProcessingMode.MODE_1);
        assertThat(result.get().requiresFep()).isTrue();
    }

    @AfterEach
    void clearBridge() {
        MessageDirectionMapBridge.clearForTest();
    }
}
