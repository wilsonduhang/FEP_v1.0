package com.puchain.fep.web.sysmgmt.config.dirmap;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirMapConfigStore;
import com.puchain.fep.processor.routing.DirectionMapping;
import com.puchain.fep.processor.routing.DynamicMessageDirectionMap;
import com.puchain.fep.processor.routing.MessageDirectionMap;
import com.puchain.fep.processor.routing.RoleDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * P3a T7 — Static fallback IT (D5: DataAccessException must not propagate to lookup callers).
 *
 * <p>v1e Hexagonal Port — mock {@link DirMapConfigStore} (Port interface in
 * fep-processor) rather than the JPA Repository (which lives in fep-web Adapter
 * layer per D7 decision). Verifies static fallback (D5) when Port throws
 * {@code DataAccessException} (subclass {@link QueryTimeoutException}).
 *
 * <p>{@code @DirtiesContext} ensures the @MockBean swap does not bleed into
 * other Spring Boot tests (especially the bridge static field on
 * {@code MessageDirectionMapBridge}).
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@DirtiesContext
class DirMapConfigFallbackIT {

    @MockBean
    private DirMapConfigStore store;  // v1e — mock the Port, not the JPA Repository

    @Autowired
    private DynamicMessageDirectionMap dynamicMap;

    /**
     * T7 quality reviewer P1-2 修复（2026-05-01）：方法名澄清 — 原名
     * {@code shouldStartUpSuccessfully_whenDbIsUnavailable} 误导（@MockBean 在
     * @PostConstruct.load() 跑完后才被 thenThrow 配置，故实际验证的是 reload-with-DB-down，
     * 不是 startup-with-DB-down）。Spring ctx 启动期间 mock 默认返回空 List，
     * dynamicMap 已成功初始化（empty cache）；本测试触发的是 reload() 路径上 Port
     * 抛异常时 cache 不被覆盖、lookup 走 fallback。语义不变，命名贴合行为。
     */
    @Test
    void shouldKeepEmptyCache_andServeStaticFallback_whenReloadFails() {
        // Mock Port throws on findAll; reload triggers re-load via Port → exception → cache stays empty
        when(store.findAll()).thenThrow(new QueryTimeoutException("DB unreachable"));
        dynamicMap.reload();

        // cacheSize == 0 证明 cache 未 loaded → lookup 必走静态 fallback
        // (mocked store.findOne also returns Optional.empty() by default → lookupRaw returns empty)
        assertThat(dynamicMap.cacheSize())
                .as("cache should be empty after DataAccessException from Port")
                .isEqualTo(0L);

        // MessageDirectionMap.lookup 仍可工作（fallback 到 88 条编译期常量）
        Optional<DirectionMapping> result = MessageDirectionMap.lookup(
                MessageType.byMsgNo("3001").orElseThrow(),
                AccessRole.ACCEPTING_ORG);
        assertThat(result)
                .as("static fallback must yield the well-known 3001/ACCEPTING_ORG direction")
                .hasValueSatisfying(m ->
                        assertThat(m.direction()).isEqualTo(RoleDirection.INBOUND_PASSIVE));
    }

    @Test
    void shouldNotPropagateDbException_toLookupCallers() {
        // 即使 Port 抛异常，lookup 必须返回结果（D5 不能让 reconciliation service 因 DB 抖动失败）
        when(store.findAll()).thenThrow(new QueryTimeoutException("transient timeout"));
        dynamicMap.reload();

        // 全部 88 条 (msg, role) 在 fallback 路径上必须可查
        for (MessageType msg : MessageDirectionMap.coveredMessages()) {
            for (AccessRole role : AccessRole.values()) {
                Optional<DirectionMapping> r = MessageDirectionMap.lookup(msg, role);
                assertThat(r)
                        .as("static fallback must cover msg=%s role=%s when DB unavailable", msg, role)
                        .isPresent();
            }
        }
    }
}
