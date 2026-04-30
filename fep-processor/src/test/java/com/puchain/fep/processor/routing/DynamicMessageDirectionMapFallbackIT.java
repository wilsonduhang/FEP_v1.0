package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 实证：DB 不可达时 {@link MessageDirectionMap#staticLookup} 88 条全部命中
 * 且与 P2c/P2d 静态结果逐条 equals。
 *
 * <p><b>v1i P0-C6 修复</b>（Plan §4 line 2305 v1h note）：fep-processor 是 lib 模块
 * 无 {@code @SpringBootApplication}，{@code @SpringBootTest} 在此模块无法启动；
 * 同时本类的核心断言只验证静态 fallback 路径（{@link MessageDirectionMap#staticLookup}），
 * 不真正调用任何 Spring-managed bean，因此采用纯 JUnit + Mockito 模式：</p>
 * <ul>
 *   <li>无 {@code @SpringBootTest} / {@code @MockBean} / {@code @DirtiesContext}</li>
 *   <li>静态 88 条覆盖 = 44 报文 × 2 角色，{@link MessageDirectionMap#coveredMessages}
 *       逐对断言 {@link MessageDirectionMap#staticLookup} present</li>
 *   <li>cold-start dynamic 路径由 sibling {@link DynamicMessageDirectionMapColdStartTest}
 *       覆盖（Plan v1h P0-ζ 拆分后契约）</li>
 * </ul>
 *
 * <p>保留 {@code *IT} 类名以符合 Plan §4 line 2244 命名契约（虽 fep-processor surefire
 * 仍按 {@code *Test.java} pattern 收集，但 {@code *IT.java} 也被本模块默认 surefire
 * 配置 includes 覆盖 — 见 fep-parent pom）。</p>
 */
class DynamicMessageDirectionMapFallbackIT {

    @AfterEach
    void clearBridge() {
        // Bridge 静态字段在 sibling test 中可能被构造函数 setDynamic 副作用污染，
        // 显式清空避免下游测试 lookup 路径错走 dynamic（D5 fallback 契约要求 Bridge null）
        MessageDirectionMapBridge.clearForTest();
    }

    @Test
    void staticFallback_covers88RowsExhaustively() {
        // 88 条静态 TABLE 必须覆盖每个 (msg, role) 组合 — D5 契约的真正语义
        for (MessageType msg : MessageDirectionMap.coveredMessages()) {
            for (AccessRole role : AccessRole.values()) {
                Optional<DirectionMapping> staticHit =
                        MessageDirectionMap.staticLookup(msg, role);
                assertThat(staticHit)
                        .as("static fallback must hit for (%s, %s)", msg, role)
                        .isPresent();
            }
        }
    }

    @Test
    void coveredMessagesContains44Distinct() {
        assertThat(MessageDirectionMap.coveredMessages()).hasSize(44);
    }
}
