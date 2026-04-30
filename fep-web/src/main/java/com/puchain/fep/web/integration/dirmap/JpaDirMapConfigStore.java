package com.puchain.fep.web.integration.dirmap;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirMapConfigSnapshot;
import com.puchain.fep.processor.routing.DirMapConfigStore;
import com.puchain.fep.processor.routing.DirMapConfigUpdate;
import com.puchain.fep.processor.routing.ProcessingMode;
import com.puchain.fep.processor.routing.RoleDirection;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * fep-web Adapter 层 — DirMapConfigStore Port 的 JPA 生产实现。
 *
 * <p>v1g P0-B 修复（Round 4 F P0-1）：v1f 漏建此类，Spring 无 @Primary Bean →
 * fallback 到 InMemoryDirMapConfigStore default → 数据不持久化。</p>
 *
 * <p>P2e {@code JpaReconciliationStore} 模式：Bean 名 {@code jpaDirMapConfigStore} 与
 * {@link com.puchain.fep.processor.routing.InMemoryDirMapConfigStore} 的
 * {@code @ConditionalOnMissingBean(name = "jpaDirMapConfigStore")} 配对生效。</p>
 *
 * <p>v1i P0-B6 修复（Round 6 J1 + K2/K5 双 reviewer 共抓）：
 * Adapter 仅 save Entity，<b>不</b>写 history（D3 单审计行）；history audit 由
 * fep-web Service 层（{@code DirMapConfigAdminService.update}）单一来源负责。</p>
 *
 * <p>更新流程（D3 + D6）：
 * <ol>
 *   <li>读旧 Entity（不存在则 fail-fast）</li>
 *   <li>写新 Entity（save = upsert）</li>
 *   <li>事务边界由 {@code @Transactional} 守护，trigger 失败时 rollback</li>
 *   <li><b>history 写入由 Service 层负责，不在本 Adapter 内</b></li>
 * </ol></p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component("jpaDirMapConfigStore")
@Primary
public class JpaDirMapConfigStore implements DirMapConfigStore {

    private final DirMapConfigJpaRepository configRepo;

    /**
     * 注入 JPA Repository。
     *
     * @param configRepo {@link DirMapConfigJpaRepository}，由 Spring Data 自动派生，非 null
     */
    public JpaDirMapConfigStore(final DirMapConfigJpaRepository configRepo) {
        this.configRepo = Objects.requireNonNull(configRepo, "configRepo");
    }

    @Override
    public List<DirMapConfigSnapshot> findAll() {
        return configRepo.findAll().stream().map(JpaDirMapConfigStore::toSnapshot).toList();
    }

    @Override
    public Optional<DirMapConfigSnapshot> findOne(final MessageType msg, final AccessRole role) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(role, "role");
        // v1i P0-A6 — MessageType 真实 API 是 msgNo() 不是 code()
        // (fep-converter/.../MessageType.java:87 实测确认)
        return configRepo.findById(new DirMapConfigKey(msg.msgNo(), role.name()))
                .map(JpaDirMapConfigStore::toSnapshot);
    }

    @Override
    public long count() {
        return configRepo.count();
    }

    @Override
    @Transactional
    public DirMapConfigSnapshot update(final DirMapConfigUpdate update) {
        Objects.requireNonNull(update, "update");
        // v1h P0-α — DirMapConfigUpdate record fields are
        // (messageType, accessRole, direction, requiresFep, mode, updatedBy) — 6 个，
        // 不是 v1g 误用的 (msg, role, newDirection, ..., reason) 7 个。
        // v1i P0-A6 — msgNo() 不是 code()
        DirMapConfigKey key = new DirMapConfigKey(
                update.messageType().msgNo(), update.accessRole().name());
        DirMapConfigEntity existing = configRepo.findById(key)
                .orElseThrow(() -> new IllegalStateException(
                        "DIR-MAP config not found for " + key.getMessageType()
                                + "/" + key.getAccessRole()));

        existing.setDirection(update.direction().name());
        existing.setRequiresFep(update.requiresFep());
        existing.setProcessingMode(update.mode().name());
        existing.setUpdatedBy(update.updatedBy());
        existing.setUpdatedAt(Instant.now());
        DirMapConfigEntity saved = configRepo.save(existing);

        // v1i P0-B6 修复（Round 6 J1 + K2/K5 双 reviewer 共抓）：
        // Adapter 不再写 history 行；history audit 由 fep-web Service 层
        // (DirMapConfigAdminService.update) 单一来源负责（含 changeReason 字段）。
        // 此前 v1g/v1h 双写 → 每次 admin update 落库 2 行 history 违 D3 单审计行约定。

        return toSnapshot(saved);
    }

    private static DirMapConfigSnapshot toSnapshot(final DirMapConfigEntity e) {
        // v1h P0-γ — MessageType.byMsgNo 返回 Optional<MessageType>，必须 .orElseThrow
        // 解包；脏数据（DB 出现 V20 seed 之外的 msg_no）情况下立即 fail-fast 而非
        // 静默 NPE。Round 5 H/I 共抓 v1g 漏此契约。
        MessageType msgType = MessageType.byMsgNo(e.getMessageType()).orElseThrow(() ->
                new IllegalStateException("Unknown messageType in DB: " + e.getMessageType()));
        return new DirMapConfigSnapshot(
                msgType,
                AccessRole.valueOf(e.getAccessRole()),
                RoleDirection.valueOf(e.getDirection()),
                e.isRequiresFep(),
                ProcessingMode.valueOf(e.getProcessingMode()),
                e.getUpdatedBy(),
                e.getUpdatedAt());
    }
}
