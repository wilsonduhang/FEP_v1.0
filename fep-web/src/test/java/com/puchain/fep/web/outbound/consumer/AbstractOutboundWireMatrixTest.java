package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

// BodyClassRegistry: same-package class（com.puchain.fep.web.outbound.consumer，无需 import）
import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * DEF-Reuse-R1（2026-05-21）— 6 outbound wire test sibling 共享父类。
 *
 * <p>抽取 6 个并行的 {@code Outbound*WireTest} sibling 共有结构（P4-MSG-I T1-T5 Deferred R1
 * HIGH ROI）:</p>
 *
 * <ul>
 *   <li>{@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 双 bean
 *       {@code @Autowired}</li>
 *   <li>{@code @TestPropertySource}（{@code fep.collector.scheduling.enabled=false} +
 *       {@code management.health.redis.enabled=false}）触发 Spring context 缓存复用</li>
 *   <li>{@code @ParameterizedTest @MethodSource("wireMatrix")} + 4 块断言（registry.resolve /
 *       describeFor headElementName + headClass + requiresResultCode）
 *       + 1 块 {@code isRegisteredOutboundMsgNo} 断言</li>
 * </ul>
 *
 * <h2>子类约定</h2>
 *
 * <p>子类必须 (1) 加 {@code @SpringBootTest} (2) {@code extends AbstractOutboundWireMatrixTest}
 * (3) 提供 {@code static Stream<Arguments> wireMatrix()} 数据源 — Arguments 5 元素：
 * {@code (msgNo, expectedBodyClass, expectedHeadElementName, expectedHeadClass,
 * expectedRequiresResultCode)}。</p>
 *
 * <h2>JUnit 5 @MethodSource lookup 机制</h2>
 *
 * <p>{@code @MethodSource("wireMatrix")} 在父类声明时，JUnit Jupiter 通过
 * {@code ReflectionUtils.findMethod} 在 test instance 实际类（即子类）lookup
 * static 方法，因此每个子类必须自带 {@code static wireMatrix()}。
 * {@code @TestPropertySource} 通过 Spring meta-annotation 继承机制由父类传到子类
 * （Spring 自动 walk class hierarchy 收集 {@code @TestPropertySource}），子类不重复声明。</p>
 *
 * <h2>测试方法 final</h2>
 *
 * <p>测试方法标记 {@code final} 防止子类覆盖 — 子类只通过提供 {@code wireMatrix()}
 * 数据扩展测试 case，不应修改断言逻辑（断言行为字节级等价于 P4-MSG-A/E/F/G/H/I
 * 既有 6 sibling）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
abstract class AbstractOutboundWireMatrixTest {

    @Autowired
    protected BodyClassRegistry registry;

    @Autowired
    protected OutboundWireShapeDispatcher dispatcher;

    @ParameterizedTest(name = "[{index}] msgNo={0} → body={1}, head={2}({3}), result={4}")
    @MethodSource("wireMatrix")
    final void wire_should_resolve_consistently(
            final String msgNo,
            final Class<?> expectedBodyClass,
            final String expectedHeadElementName,
            final Class<? extends RequestBusinessHead> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        // BodyClassRegistry: msgNo -> Body POJO Class
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s",
                        msgNo, expectedBodyClass.getSimpleName())
                .isEqualTo(expectedBodyClass);

        // OutboundWireShapeDispatcher: msgNo -> wire-shape 描述符
        final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
        assertThat(desc.headElementName())
                .as("msgNo=%s headElementName（与 %s.xsd 一致）", msgNo, msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(desc.headClass())
                .as("msgNo=%s headClass", msgNo)
                .isEqualTo(expectedHeadClass);
        assertThat(desc.requiresResultCode())
                .as("msgNo=%s requiresResultCode", msgNo)
                .isEqualTo(expectedRequiresResultCode);

        // BatchMessageProcessorService.resolveHeadElementName 路径
        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s isRegisteredOutboundMsgNo 必须 true（否则 inbound 走 legacy fallback）",
                        msgNo)
                .isTrue();
    }
}
