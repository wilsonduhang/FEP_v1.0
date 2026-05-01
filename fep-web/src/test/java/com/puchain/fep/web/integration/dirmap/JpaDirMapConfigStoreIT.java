package com.puchain.fep.web.integration.dirmap;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirMapConfigSnapshot;
import com.puchain.fep.processor.routing.DirMapConfigUpdate;
import com.puchain.fep.processor.routing.MessageDirectionMapBridge;
import com.puchain.fep.processor.routing.ProcessingMode;
import com.puchain.fep.processor.routing.RoleDirection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3a T2 — JpaDirMapConfigStore Adapter 集成测试。
 *
 * <p>v1g P0-B：实证 88 行 findAll + findOne + update 走 JPA 生产路径。
 * v1i P0-E6：Adapter 不再写 history（D3 单审计行约定），断言 historyBefore 不变。</p>
 *
 * <p>{@code @DirtiesContext(AFTER_CLASS)} — 防 sibling test 中 setDynamic 静态字段
 * 污染（v1h P0-ε，配合 @AfterEach Bridge.clearForTest 双保险）。</p>
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JpaDirMapConfigStoreIT {

    @Autowired
    private JpaDirMapConfigStore store;

    @Autowired
    private DirMapConfigHistoryRepository historyRepo;

    @Test
    void shouldLoad88RowsFromV20Seed() {
        List<DirMapConfigSnapshot> all = store.findAll();
        assertThat(all).hasSize(88);
        assertThat(store.count()).isEqualTo(88L);
    }

    @Test
    void shouldFindOneByMessageTypeAndRole_andReturnSnapshot() {
        Optional<DirMapConfigSnapshot> result = store.findOne(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(result).isPresent();
    }

    @Test
    void shouldUpdateConfig_withoutWritingHistory_v1i() {
        // v1i P0-B6 / P0-E6 — Adapter 不再写 history（D3 单审计行决策由
        // DirMapConfigAdminService.update 单一来源负责）。本 IT 直调 store.update()
        // 跳过 Service 链路，因此 history 表行数不应变化。Service 链路的 history 写入
        // 由 T5 IT (DirMapConfigAdminServiceIT) 验证。
        //
        // T7 quality reviewer P0-1 修复（2026-05-01）：try/finally 保证还原 MSG_3001
        // 至 V20 静态基线（INBOUND_PASSIVE/MODE_1）。H2 dev profile 用
        // DB_CLOSE_DELAY=-1（application-test.yml）跨 ctx 持久化，本测试此前未还原 →
        // sibling DirMapConfigControllerIT.shouldMatchStaticBaseline_eachOf88Rows
        // 在同次 mvn 运行中 FAIL（baseline 看到 OUTBOUND_ACTIVE/MODE_2 而非
        // INBOUND_PASSIVE/MODE_1）。
        long historyBefore = historyRepo.count();
        DirMapConfigUpdate update = new DirMapConfigUpdate(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                RoleDirection.OUTBOUND_ACTIVE, true, ProcessingMode.MODE_2,
                "admin1");
        try {
            DirMapConfigSnapshot after = store.update(update);

            assertThat(after.direction()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE);
            assertThat(after.mode()).isEqualTo(ProcessingMode.MODE_2);
            assertThat(historyRepo.count())
                    .as("Adapter 不应写 history（v1i P0-B6）")
                    .isEqualTo(historyBefore);
        } finally {
            // 还原至 V20 baseline（INBOUND_PASSIVE/MODE_1, requiresFep=true, system）
            store.update(new DirMapConfigUpdate(
                    MessageType.MSG_3001, AccessRole.ACCEPTING_ORG,
                    RoleDirection.INBOUND_PASSIVE, true, ProcessingMode.MODE_1,
                    "system"));
        }
    }

    @Test
    void historyRepoStillFunctional_directWriteVerifiesSchema() {
        // v1i α — Adapter 删 history 写后，本 IT 仍直调 historyRepo.save 验证
        // schema 工作（保住 historyRepo 在 fep-web Adapter 层的契约可用性）
        long before = historyRepo.count();
        DirMapConfigHistoryEntity row = new DirMapConfigHistoryEntity();
        row.setHistoryId(java.util.UUID.randomUUID().toString().replace("-", ""));
        row.setMessageType("3001");
        row.setAccessRole("ACCEPTING_ORG");
        row.setOldDirection("INBOUND_PASSIVE");
        row.setOldRequiresFep(true);
        row.setOldMode("MODE_1");
        row.setNewDirection("OUTBOUND_ACTIVE");
        row.setNewRequiresFep(true);
        row.setNewMode("MODE_2");
        row.setChangedBy("admin1");
        row.setChangedAt(java.time.Instant.parse("2026-04-30T00:00:00Z"));
        row.setChangeReason("v1i schema verify");
        historyRepo.save(row);
        assertThat(historyRepo.count()).isEqualTo(before + 1);
    }

    @AfterEach
    void clearBridge() {
        // v1h P0-ε — P0-A `setDynamic(this)` 副作用使本 IT 启动期 dynamicMap bean
        // 写入 Bridge 静态字段；ctx 关闭后该字段仍指向已停 ctx 的 bean，污染下游
        // 同模块 @SpringBootTest（如 DirMapConfigHistoryRepositoryIT）。每用例
        // tear-down 清空，配合 @DirtiesContext(AFTER_CLASS) 双保险。
        MessageDirectionMapBridge.clearForTest();
    }
}
