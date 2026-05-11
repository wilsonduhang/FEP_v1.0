package com.puchain.fep.architecture;

import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E-3 T4 — 强护栏：{@code InboundMessageDispatcher.BODY_TYPE_REGISTRY}
 * 注册的所有 Body class 必须 {@code implements SerialNoBearing}。
 *
 * <p>未来 P4 / P5 阶段注册新 inbound body 时，若漏 implements，本测试立即抓到。
 * 现状（2026-05-11）注册 10 个：2101（HNDEMP→外联 数据推送，P4-MSG-D T4，返回 null）
 * + 2102/2103/2104（BATCH 响应，T2-ext 实现返回 null）
 * + 3007/3008/3107/3108/3115/3116（单条业务体，T2 base 实现返回 SerialNo 字段）。</p>
 *
 * <p><b>实现策略</b>: 不用 ArchUnit DSL（DSL 描述"所有被 X 引用的类"较绕），直接读
 * dispatcher 暴露的 {@link InboundMessageDispatcher#bodyTypeRegistry()} 公开方法，
 * 逐一断言 isAssignableFrom。</p>
 */
class InboundRegistryArchTest {

    @Test
    void allRegisteredBodies_mustImplementSerialNoBearing() {
        Map<String, Class<?>> registry = InboundMessageDispatcher.bodyTypeRegistry();
        assertThat(registry).isNotEmpty();

        List<String> violations = registry.entrySet().stream()
                .filter(e -> !SerialNoBearing.class.isAssignableFrom(e.getValue()))
                .map(e -> "msgNo=" + e.getKey()
                        + " class=" + e.getValue().getSimpleName()
                        + " not implements SerialNoBearing")
                .collect(Collectors.toList());

        assertThat(violations)
                .as("BODY_TYPE_REGISTRY entries missing SerialNoBearing implementation")
                .isEmpty();
    }

    @Test
    void registry_currentSnapshot_hasTenEntries() {
        // Snapshot guard — 提示开发者注册新 body 时同步更新 SerialNoBearingComplianceTest
        // 10 项 = 1 推送（2101，null fallback）+ 3 BATCH（2102/2103/2104，null fallback）
        //   + 6 单条（3007/3008/3107/3108/3115/3116，返回 SerialNo 字段）
        Map<String, Class<?>> registry = InboundMessageDispatcher.bodyTypeRegistry();
        assertThat(registry).hasSize(10);
        assertThat(registry).containsKeys(
                "2101",
                "2102", "2103", "2104",
                "3007", "3008", "3107", "3108", "3115", "3116");
    }
}
