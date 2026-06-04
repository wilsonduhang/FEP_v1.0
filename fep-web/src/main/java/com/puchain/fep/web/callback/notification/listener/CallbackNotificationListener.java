package com.puchain.fep.web.callback.notification.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 回调死信告警监听器：订阅 {@link CallbackDeadLetterEvent}，向每个管理员（role={@code ADMIN}）
 * 落一条站内通知。
 *
 * <p>管理员定位采用三步查询（red line {@code feedback_plan_must_grep_actual_api}：
 * {@code SysUserRepository.findByRoleCode} 不存在，故拆解）：
 * (1) {@link SysRoleRepository#findByRoleCode} 取 ADMIN 角色 →
 * (2) {@link SysUserRoleRepository#findByRoleId} 取用户-角色关联 →
 * (3) {@link SysUserRepository#findAllById} 批量加载用户。事件与发送解耦，站内通知为当前唯一
 * 落地目标，EMAIL/SMS 为 Phase 2c 扩展点。参见 PRD v1.3 §5.5.3 回调可靠性告警
 * （FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "queueId passed through LogSanitizer.sanitize() prior to LOG.warn")
public class CallbackNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackNotificationListener.class);
    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String CATEGORY_DLQ = "CALLBACK_DLQ";
    private static final String LEVEL_ERROR = "ERROR";
    private static final String REF_TYPE_DLQ = "CALLBACK_DLQ_ENTRY";

    private final SysRoleRepository roleRepo;
    private final SysUserRoleRepository userRoleRepo;
    private final SysUserRepository userRepo;
    private final CallbackNotificationRepository notifRepo;

    /**
     * 构造死信告警监听器。
     *
     * @param roleRepo     角色仓储（定位 ADMIN 角色）
     * @param userRoleRepo 用户-角色关联仓储
     * @param userRepo     用户仓储
     * @param notifRepo    站内通知仓储
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed repository singletons stored by reference per container contract")
    public CallbackNotificationListener(final SysRoleRepository roleRepo,
            final SysUserRoleRepository userRoleRepo, final SysUserRepository userRepo,
            final CallbackNotificationRepository notifRepo) {
        this.roleRepo = roleRepo;
        this.userRoleRepo = userRoleRepo;
        this.userRepo = userRepo;
        this.notifRepo = notifRepo;
    }

    /**
     * 处理回调死信事件：为每个管理员落一条站内通知。
     *
     * <p>无 ADMIN 角色或无管理员用户时记 WARN 并安全返回（不抛异常，避免影响死信主流程）。</p>
     *
     * @param ev 回调死信事件
     */
    @EventListener
    @Transactional
    public void onDeadLetter(final CallbackDeadLetterEvent ev) {
        final Optional<SysRole> adminRole = roleRepo.findByRoleCode(ADMIN_ROLE_CODE);
        if (adminRole.isEmpty()) {
            LOG.warn("DLQ event but ADMIN role not configured, queueId={}",
                    LogSanitizer.sanitize(ev.queueId()));
            return;
        }
        final List<String> adminUserIds = userRoleRepo.findByRoleId(adminRole.get().getRoleId())
                .stream().map(SysUserRole::getUserId).toList();
        if (adminUserIds.isEmpty()) {
            LOG.warn("DLQ event but no users assigned ADMIN role, queueId={}",
                    LogSanitizer.sanitize(ev.queueId()));
            return;
        }
        final List<SysUser> admins = userRepo.findAllById(adminUserIds);
        for (final SysUser u : admins) {
            notifRepo.save(CallbackNotificationEntity.of(
                    u.getUserId(),
                    CATEGORY_DLQ,
                    LEVEL_ERROR,
                    "回调死信 - " + ev.targetInterfaceId(),
                    String.format("queueId=%s msgNo=%s retryCount=%d error=%s",
                            ev.queueId(), ev.msgNo(), ev.retryCount(), ev.lastError()),
                    ev.queueId(),
                    REF_TYPE_DLQ));
        }
    }
}
