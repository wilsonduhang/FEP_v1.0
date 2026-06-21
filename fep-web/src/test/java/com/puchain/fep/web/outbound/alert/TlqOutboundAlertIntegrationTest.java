package com.puchain.fep.web.outbound.alert;

import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.event.InAppNotificationCreatedEvent;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import com.puchain.fep.web.outbound.event.TlqOutboundDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.RoleStatus;
import com.puchain.fep.web.sysmgmt.role.domain.RoleType;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.domain.UserStatus;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * B-9 TLQ 出站死信告警端到端集成测试（FR-WEB-TLQ-FAULT）。
 *
 * <p>验证完整链路：发布 {@link TlqOutboundDeadLetterEvent} →
 * {@link TlqOutboundAlertEvaluator}（按默认 {@code t_sys_alert_rule} 配置 enabled/threshold=0/IN_APP）→
 * {@code CallbackInAppAlertChannel} 为 ADMIN 用户写 {@code in_app_notification}（category=TLQ_OUTBOUND_DLQ）
 * → 发布 {@link InAppNotificationCreatedEvent}（B-8 实时推送链触发）。</p>
 *
 * <p>沿用 {@code CallbackPhase2bEndToEndIT} 约定：依赖 V6+V34 迁移种入的默认告警规则
 * （enabled / threshold=0 / IN_APP / REALTIME），仅 seed ADMIN 用户；非事务测试使 save 提交可见。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@RecordApplicationEvents
@TestPropertySource(properties = {
        "fep.transport.provider=mock",
        "fep.collector.institution-code=12345678901234",
        "fep.collector.scheduling.enabled=false",
        "fep.outbound.queue.poll-interval-ms=99999",
        "fep.outbound.queue.poll-initial-delay-ms=99999",
        "fep.callback.poll-interval-ms=600000",
        "fep.callback.poll-initial-delay-ms=600000",
        "management.health.redis.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("B-9 TLQ 出站死信告警 E2E — event→evaluator→IN_APP→推送事件")
class TlqOutboundAlertIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ApplicationEvents applicationEvents;
    @Autowired
    private CallbackNotificationRepository notificationRepository;
    @Autowired
    private SysRoleRepository sysRoleRepository;
    @Autowired
    private SysUserRepository sysUserRepository;
    @Autowired
    private SysUserRoleRepository sysUserRoleRepository;

    private String seededAdminUserId;
    private Long seededUserRoleId;
    private String createdRoleId;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        seedAdminUser();
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        if (seededUserRoleId != null) {
            sysUserRoleRepository.findById(seededUserRoleId).ifPresent(sysUserRoleRepository::delete);
        }
        if (seededAdminUserId != null) {
            sysUserRepository.findById(seededAdminUserId).ifPresent(sysUserRepository::delete);
        }
        if (createdRoleId != null) {
            sysRoleRepository.findById(createdRoleId).ifPresent(sysRoleRepository::delete);
        }
    }

    /** Seed (find-or-create) an ADMIN role + a unique admin user linked to it. */
    private void seedAdminUser() {
        final Optional<SysRole> existing = sysRoleRepository.findByRoleCode("ADMIN");
        final String roleId;
        if (existing.isPresent()) {
            roleId = existing.get().getRoleId();
        } else {
            final SysRole role = new SysRole();
            roleId = UUID.randomUUID().toString().replace("-", "");
            role.setRoleId(roleId);
            role.setRoleCode("ADMIN");
            role.setRoleName("B9 Admin");
            role.setRoleType(RoleType.SYSTEM);
            role.setRoleStatus(RoleStatus.ACTIVE);
            sysRoleRepository.save(role);
            createdRoleId = roleId;
        }

        final SysUser admin = new SysUser();
        seededAdminUserId = UUID.randomUUID().toString().replace("-", "");
        admin.setUserId(seededAdminUserId);
        admin.setUserAccount("admin-b9-" + System.nanoTime());
        admin.setUserName("B9 Admin User");
        admin.setPasswordHash("x");
        admin.setUserStatus(UserStatus.ACTIVE);
        admin.setLoginFailCount(0);
        sysUserRepository.save(admin);

        final SysUserRole link = new SysUserRole(seededAdminUserId, roleId);
        sysUserRoleRepository.save(link);
        seededUserRoleId = link.getId();
    }

    @Test
    @DisplayName("TLQ 出站死信事件 → ADMIN 站内通知(TLQ_OUTBOUND_DLQ) + 推送事件")
    void tlqOutboundDeadLetter_createsTlqCategoryNotification_andPublishesPushEvent() {
        final String queueId = "Q-B9-" + System.nanoTime();

        eventPublisher.publishEvent(new TlqOutboundDeadLetterEvent(
                queueId, null, 5, "TLQ send fail", LocalDateTime.now()));

        // 1. 站内通知为 seeded admin 落库，category=TLQ_OUTBOUND_DLQ。
        final List<CallbackNotificationEntity> notifs =
                notificationRepository.findByUserIdAndReadFalseOrderByCreateTimeDesc(seededAdminUserId);
        Assertions.assertThat(notifs)
                .as("seeded admin should have one unread TLQ outbound DLQ notification")
                .hasSize(1);
        final CallbackNotificationEntity notif = notifs.get(0);
        Assertions.assertThat(notif.getCategory()).isEqualTo("TLQ_OUTBOUND_DLQ");
        Assertions.assertThat(notif.getRefId())
                .as("notification refId should point at the dead-letter queue id")
                .isEqualTo(queueId);

        // 2. B-8 推送链触发：发布了 InAppNotificationCreatedEvent（携带该 admin + notificationId）。
        final List<InAppNotificationCreatedEvent> pushEvents =
                applicationEvents.stream(InAppNotificationCreatedEvent.class).toList();
        Assertions.assertThat(pushEvents)
                .as("one push event per created notification for the seeded admin")
                .anyMatch(e -> e.userId().equals(seededAdminUserId)
                        && e.notificationId().equals(notif.getNotificationId()));
    }
}
