package com.puchain.fep.web.sysmgmt.config.dirmap.service;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.DirMapConfigChangedEvent;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirMapConfigSnapshot;
import com.puchain.fep.processor.routing.DirMapConfigStore;
import com.puchain.fep.processor.routing.DirMapConfigUpdate;
import com.puchain.fep.processor.routing.ProcessingMode;
import com.puchain.fep.processor.routing.RoleDirection;
import com.puchain.fep.web.common.SecurityContextHelper;
import com.puchain.fep.web.integration.dirmap.DirMapConfigHistoryEntity;
import com.puchain.fep.web.integration.dirmap.DirMapConfigHistoryRepository;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigResponse;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapHistoryResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * v1f Hexagonal — Service inject {@link DirMapConfigStore} Port (fep-processor)
 * 而非 fep-processor JPA Repository（v1d 错配，已改 fep-web Adapter 层）。
 * History audit 写入仍由本 Service 直接 inject fep-web 端的
 * {@link DirMapConfigHistoryRepository}（与 P2e ReconciliationEventListener 模式一致 —
 * Adapter 边界事件由 fep-web 应用层负责）。
 */
@Service
public class DirMapConfigAdminService {

    /** D8 invariant: {@code t_dir_map_config} must always hold this many rows. */
    private static final long EXPECTED_ROW_COUNT = 88L;

    private final DirMapConfigStore configStore;             // v1f Port (fep-processor)
    private final DirMapConfigHistoryRepository historyRepo; // v1f fep-web Adapter
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Construct service with port + audit repo + event publisher.
     *
     * @param configStore    Port to {@code t_dir_map_config} (fep-processor)
     * @param historyRepo    JPA repo for {@code t_dir_map_config_history} (fep-web)
     * @param eventPublisher Spring event bus for cache invalidation
     */
    public DirMapConfigAdminService(
            DirMapConfigStore configStore,
            DirMapConfigHistoryRepository historyRepo,
            ApplicationEventPublisher eventPublisher) {
        this.configStore = configStore;
        this.historyRepo = historyRepo;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Return all 88 rows sorted by {@code messageType} then {@code accessRole}.
     * Pagination is handled by the controller (1-based pageNum adapter).
     *
     * @return immutable list of {@link DirMapConfigResponse}, never null
     */
    public List<DirMapConfigResponse> listAll() {
        // Total rows fixed by EXPECTED_ROW_COUNT — controller handles 0/1-based slicing
        return configStore.findAll().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(DirMapConfigResponse::messageType)
                        .thenComparing(DirMapConfigResponse::accessRole))
                .toList();
    }

    /**
     * Update one DIR-MAP row in a transaction: write history audit row, mutate
     * the live config via {@link DirMapConfigStore}, and publish
     * {@link DirMapConfigChangedEvent} for downstream cache invalidation.
     *
     * <p>D8 invariant ({@value #EXPECTED_ROW_COUNT} rows) is asserted before
     * and after the mutation; violation triggers transaction rollback.
     *
     * @param messageType message type code, e.g. {@code "3001"}
     * @param accessRole  {@link AccessRole} name
     * @param req         non-null request body (validated by Controller)
     * @return updated row snapshot
     * @throws IllegalStateException    if the row-count invariant is violated
     * @throws IllegalArgumentException if any enum value is unknown or the
     *                                  target row does not exist
     */
    @Transactional
    public DirMapConfigResponse update(
            String messageType, String accessRole, DirMapConfigUpdateRequest req) {
        // D8 应用层兜底（schema-level trigger 是真不变性守护，本 Assert 提前提示用户错误）
        long preCount = configStore.count();
        if (preCount != EXPECTED_ROW_COUNT) {
            throw new IllegalStateException(
                    "DIR-MAP invariant violated: t_dir_map_config count=" + preCount
                  + " expected=" + EXPECTED_ROW_COUNT
                  + ". Refusing to update; investigate before proceeding.");
        }

        // enum 解析（valueOf 抛 IllegalArgumentException 触发 Controller 400）
        MessageType msg = MessageType.byMsgNo(messageType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown messageType: " + messageType));
        AccessRole role = AccessRole.valueOf(accessRole);
        RoleDirection newDir = RoleDirection.valueOf(req.direction());
        ProcessingMode newMode = ProcessingMode.valueOf(req.processingMode());

        // 取 old snapshot via Port
        DirMapConfigSnapshot before = configStore.findOne(msg, role).orElseThrow(
                () -> new IllegalArgumentException(
                        "DIR-MAP config not found: " + messageType + "/" + accessRole));

        // 写 history（fep-web Adapter 直接调，因 history 是 fep-web 应用层职责）
        DirMapConfigHistoryEntity history = new DirMapConfigHistoryEntity();
        history.setHistoryId(IdGenerator.uuid32());
        history.setMessageType(messageType);
        history.setAccessRole(accessRole);
        history.setOldDirection(before.direction().name());
        history.setOldRequiresFep(before.requiresFep());
        history.setOldMode(before.mode().name());
        history.setNewDirection(req.direction());
        history.setNewRequiresFep(req.requiresFep());
        history.setNewMode(req.processingMode());
        history.setChangedBy(currentUsername());
        history.setChangedAt(Instant.now());
        history.setChangeReason(req.changeReason());
        historyRepo.save(history);

        // 通过 Port 更新（Adapter 内部走 JPA save 并尊重 schema trigger）
        // v1h P0-δ — DirMapConfigUpdate record 6 字段 (messageType, accessRole,
        // direction, requiresFep, mode, updatedBy)；v1g 误传 7 args（含多余
        // Instant.now()）已修。updatedAt 由 Adapter 内部 setUpdatedAt(Instant.now()) 兜底。
        DirMapConfigSnapshot after = configStore.update(new DirMapConfigUpdate(
                msg, role, newDir, req.requiresFep(), newMode, currentUsername()));

        // D8 操作后再次实测 count（应仍为 EXPECTED_ROW_COUNT，trigger 兜底失败时此 Assert 抛错事务回滚）
        long postCount = configStore.count();
        if (postCount != EXPECTED_ROW_COUNT) {
            throw new IllegalStateException(
                    "DIR-MAP invariant violated post-update: count=" + postCount
                  + ". Transaction will roll back (schema trigger should have caught earlier).");
        }

        // publish — DirMapCacheInvalidator 监听
        eventPublisher.publishEvent(
                new DirMapConfigChangedEvent(this, msg, role,
                        before.direction(), newDir, currentUsername()));

        return toResponse(after);
    }

    /**
     * Fetch history audit rows for a single config key, ordered newest first.
     *
     * @param messageType message type code, e.g. {@code "3001"}
     * @param accessRole  {@link AccessRole} name
     * @return immutable list of {@link DirMapHistoryResponse}, never null
     */
    public List<DirMapHistoryResponse> history(String messageType, String accessRole) {
        return historyRepo
                .findByMessageTypeAndAccessRoleOrderByChangedAtDesc(messageType, accessRole)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private DirMapConfigResponse toResponse(DirMapConfigSnapshot s) {
        return new DirMapConfigResponse(
                s.messageType().msgNo(),
                s.messageType().displayName(),
                s.accessRole().name(),
                s.direction().name(),
                s.requiresFep(),
                s.mode().name(),
                s.updatedBy(),
                s.updatedAt());
    }

    private DirMapHistoryResponse toHistoryResponse(DirMapConfigHistoryEntity h) {
        return new DirMapHistoryResponse(
                h.getHistoryId(),
                h.getOldDirection(), h.getOldRequiresFep(), h.getOldMode(),
                h.getNewDirection(), h.getNewRequiresFep(), h.getNewMode(),
                h.getChangedBy(), h.getChangedAt(), h.getChangeReason());
    }

    /**
     * Audit-trail user identifier for DirMap history rows. Reuses
     * {@link SecurityContextHelper#currentUserId()} for consistency with the
     * rest of fep-web, but maps the helper's "anonymous" sentinel to "system"
     * so audit rows show "system" (an automated/bypass-auth caller) rather
     * than "anonymous" (an unauthenticated principal). The {@code SYSTEM_ADMIN}
     * role gate ensures real production callers are always named users.
     *
     * @return audit-trail identifier, never null
     */
    private String currentUsername() {
        String userId = SecurityContextHelper.currentUserId();
        return "anonymous".equals(userId) ? "system" : userId;
    }
}
