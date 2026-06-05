package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * IN_APP 告警渠道：为每个 ADMIN 角色用户落一条站内通知（承接 Phase 2b
 * {@code CallbackNotificationListener} 的扇出逻辑，行为保持）。
 *
 * <p>ADMIN 定位三步查询（{@code SysUserRepository.findByRoleCode} 不存在）：
 * {@link SysRoleRepository#findByRoleCode} → {@link SysUserRoleRepository#findByRoleId} →
 * {@link SysUserRepository#findAllById}。参见 PRD v1.3 §5.5.3 / §5.10.7.2d
 * （FR-INFRA-CALLBACK-IN-APP-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "refId passed through LogSanitizer.sanitize() prior to LOG.warn")
public class CallbackInAppAlertChannel implements CallbackAlertChannel {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackInAppAlertChannel.class);
    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String CATEGORY_DLQ = "CALLBACK_DLQ";

    private final SysRoleRepository roleRepo;
    private final SysUserRoleRepository userRoleRepo;
    private final SysUserRepository userRepo;
    private final CallbackNotificationRepository notifRepo;

    /**
     * @param roleRepo     角色仓储
     * @param userRoleRepo 用户-角色关联仓储
     * @param userRepo     用户仓储
     * @param notifRepo    站内通知仓储
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed repository singletons stored by reference per container contract")
    public CallbackInAppAlertChannel(final SysRoleRepository roleRepo,
            final SysUserRoleRepository userRoleRepo, final SysUserRepository userRepo,
            final CallbackNotificationRepository notifRepo) {
        this.roleRepo = roleRepo;
        this.userRoleRepo = userRoleRepo;
        this.userRepo = userRepo;
        this.notifRepo = notifRepo;
    }

    @Override
    public boolean supports(final NotifyMethod method) {
        return method == NotifyMethod.IN_APP;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(final CallbackAlertMessage message) {
        final Optional<SysRole> adminRole = roleRepo.findByRoleCode(ADMIN_ROLE_CODE);
        if (adminRole.isEmpty()) {
            LOG.warn("DLQ alert but ADMIN role not configured, refId={}",
                    LogSanitizer.sanitize(message.refId()));
            return;
        }
        final List<String> adminUserIds = userRoleRepo.findByRoleId(adminRole.get().getRoleId())
                .stream().map(SysUserRole::getUserId).toList();
        if (adminUserIds.isEmpty()) {
            LOG.warn("DLQ alert but no users assigned ADMIN role, refId={}",
                    LogSanitizer.sanitize(message.refId()));
            return;
        }
        final List<SysUser> admins = userRepo.findAllById(adminUserIds);
        for (final SysUser u : admins) {
            notifRepo.save(CallbackNotificationEntity.of(
                    u.getUserId(), CATEGORY_DLQ, message.level(),
                    message.title(), message.body(), message.refId(), message.refType()));
        }
    }
}
