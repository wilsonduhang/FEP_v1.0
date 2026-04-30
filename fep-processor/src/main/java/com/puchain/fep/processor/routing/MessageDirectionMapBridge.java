package com.puchain.fep.processor.routing;

import org.springframework.stereotype.Component;

/**
 * Static bridge from {@link MessageDirectionMap}'s static {@code lookup} API
 * to the Spring-managed {@link DynamicMessageDirectionMap} bean.
 *
 * <p>{@link DynamicMessageDirectionMap} explicitly calls {@link #setDynamic} in its
 * constructor — no {@code ApplicationContextAware} reflection, no per-call
 * {@code ctx.getBean(...)} cost. In tests without Spring context the static
 * field stays null and {@link MessageDirectionMap#lookup} falls back to its
 * compile-time TABLE.</p>
 *
 * <p><b>Multi-ctx safety</b>: Tests starting/stopping multiple Spring contexts
 * within the same JVM must use {@code @DirtiesContext} or call
 * {@link #clearForTest()} in teardown to avoid cross-test bleed.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public final class MessageDirectionMapBridge {

    private static volatile DynamicMessageDirectionMap dynamic;

    /**
     * 由 {@link DynamicMessageDirectionMap} 构造函数显式调用注入。
     *
     * @param d Spring-managed dynamic map instance（构造期非 null；多 ctx 场景下后入者覆盖）
     */
    public static void setDynamic(final DynamicMessageDirectionMap d) {
        dynamic = d;
    }

    /**
     * @return Spring-managed dynamic map, or {@code null} if not yet initialized
     *     (pre-startup, test without Spring ctx, or after {@link #clearForTest}).
     */
    static DynamicMessageDirectionMap getDynamicOrNull() {
        return dynamic;
    }

    /**
     * 测试 teardown 用：清除 static 字段，避免 multi-ctx 测试场景下被前一个 ctx
     * 的 bean 实例污染。生产代码不调用。
     */
    public static void clearForTest() {
        dynamic = null;
    }

    /**
     * 公开默认构造函数供 Spring 容器实例化。本类无实例状态（仅持有 static 字段），
     * 实例化纯粹是为了让 {@code @Component} 注册到 ApplicationContext 中以参与
     * {@code @DependsOn("messageDirectionMapBridge")} 初始化顺序约束（T3
     * {@link DynamicMessageDirectionMap} 依赖）。
     */
    public MessageDirectionMapBridge() {
        // intentionally empty
    }
}
