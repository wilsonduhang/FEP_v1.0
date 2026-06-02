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
 * 现状（2026-06-02，P4-MSG-M 同步）注册 24 个：
 * 2101（P4-MSG-D T4 数据推送，T2-ext 实现返回 null）+
 * 2102/2103/2104（P4-MSG-A-inbound BATCH 响应，T2-ext 实现返回 null）+
 * 3001/3002/3003/3004/3005/3006（P4-Plan-C SUPPLY_CHAIN BIDIRECTIONAL 业务体，
 *   T2 base 实现返回 SerialNo 字段）+
 * 3007/3008/3107/3108/3115/3116（P4-MSG-B-inbound / P3 Phase 2 单条业务体，
 *   T2 base 实现返回 SerialNo 字段）+
 * 3112（核心企业授信查询请求，银行被动接收 模式5，P4-MSG-J，T2 base 实现返回 SerialNo 字段）+
 * 3009/3103/3105/3113（P4-MSG-K inbound 受理：融资结果登记 / 融资企业建档回执 /
 *   融资申请 / 核心企业授信查询回执，均实现返回 SerialNo 字段）+
 * 9007/9009（P4-MSG-L 节点登录/登出回执，无业务 SerialNo 字段，实现返回 null）+
 * 9020（P4-MSG-M 实时业务通用应答，无业务 SerialNo 字段，实现返回 null）。</p>
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
    void registry_currentSnapshot_hasTwentyFourEntries() {
        // Snapshot guard — 提示开发者注册新 body 时同步更新 SerialNoBearingComplianceTest.
        // 24 项 = 1 P4-MSG-D（2101，null fallback）+ 3 P4-MSG-A-inbound BATCH（2102/2103/2104,
        // null fallback）+ 6 P4-Plan-C（3001/3002/3003/3004/3005/3006，单条业务体）+
        // 6 P4-MSG-B-inbound / P3 Phase 2（3007/3008/3107/3108/3115/3116，单条业务体）
        // + 1 P4-MSG-J（3112，银行被动接收 模式5，SerialNo 字段）
        // + 4 P4-MSG-K（3009/3103/3105/3113，inbound 受理，SerialNo 字段）
        // + 2 P4-MSG-L（9007/9009，节点登录/登出回执，无 SerialNo 字段 null fallback）
        // + 1 P4-MSG-M（9020，实时业务通用应答，无 SerialNo 字段 null fallback）。
        Map<String, Class<?>> registry = InboundMessageDispatcher.bodyTypeRegistry();
        assertThat(registry).hasSize(24);
        assertThat(registry).containsKeys(
                "2101", "2102", "2103", "2104",
                "3001", "3002", "3003", "3004", "3005", "3006",
                "3007", "3008", "3107", "3108", "3115", "3116", "3112",
                "3009", "3103", "3105", "3113", "9007", "9009", "9020");
    }
}
