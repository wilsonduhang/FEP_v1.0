package com.puchain.fep.web.integration.dirmap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P3a T2 — DirMapConfigHistoryRepository 集成测试。
 *
 * <p>{@code @SpringBootTest} 全 classpath（**非** {@code @DataJpaTest} sliced test）—
 * 避免 H2 trigger ClassLoader 风险。验证按 (messageType, accessRole) 倒序返回
 * 最新条在前。</p>
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
class DirMapConfigHistoryRepositoryIT {

    @Autowired
    private DirMapConfigHistoryRepository repo;

    @Test
    void shouldReturnLatestFirst_whenMultipleHistoryRowsForSameTarget() {
        DirMapConfigHistoryEntity older = newHistory(
                "3001", "ACCEPTING_ORG",
                "OUTBOUND_ACTIVE", "INBOUND_PASSIVE",
                Instant.parse("2026-04-01T00:00:00Z"));
        DirMapConfigHistoryEntity newer = newHistory(
                "3001", "ACCEPTING_ORG",
                "INBOUND_PASSIVE", "OUTBOUND_ACTIVE",
                Instant.parse("2026-04-29T00:00:00Z"));
        repo.save(older);
        repo.save(newer);

        List<DirMapConfigHistoryEntity> result = repo
                .findByMessageTypeAndAccessRoleOrderByChangedAtDesc("3001", "ACCEPTING_ORG");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getChangedAt()).isEqualTo(newer.getChangedAt());
        assertThat(result.get(1).getChangedAt()).isEqualTo(older.getChangedAt());
    }

    private DirMapConfigHistoryEntity newHistory(
            final String msgType, final String role,
            final String oldDir, final String newDir, final Instant at) {
        DirMapConfigHistoryEntity e = new DirMapConfigHistoryEntity();
        e.setHistoryId(UUID.randomUUID().toString().replace("-", ""));
        e.setMessageType(msgType);
        e.setAccessRole(role);
        e.setOldDirection(oldDir);
        e.setOldRequiresFep(true);
        e.setOldMode("MODE_1");
        e.setNewDirection(newDir);
        e.setNewRequiresFep(true);
        e.setNewMode("MODE_1");
        e.setChangedBy("admin1");
        e.setChangedAt(at);
        e.setChangeReason("test");
        return e;
    }
}
