package com.puchain.fep.web.callback.notification.listener;

import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 单元测试 for {@link CallbackNotificationListener}。
 *
 * <p>mock 4 个仓储隔离持久层，验证三步管理员定位查询 + 每管理员一条通知，
 * 及无 ADMIN 角色 / 无管理员用户的安全短路（不落通知、不抛异常）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackNotificationListenerTest {

    @Mock
    private SysRoleRepository roleRepo;
    @Mock
    private SysUserRoleRepository userRoleRepo;
    @Mock
    private SysUserRepository userRepo;
    @Mock
    private CallbackNotificationRepository notifRepo;
    @InjectMocks
    private CallbackNotificationListener listener;

    private static CallbackDeadLetterEvent event() {
        return new CallbackDeadLetterEvent("Q1", "IF-001", "9001", 5, "io timeout", LocalDateTime.now());
    }

    private static SysRole role(final String roleId, final String roleCode) {
        final SysRole r = new SysRole();
        r.setRoleId(roleId);
        r.setRoleCode(roleCode);
        return r;
    }

    private static SysUserRole userRole(final String userId, final String roleId) {
        final SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        return ur;
    }

    private static SysUser user(final String userId) {
        final SysUser u = new SysUser();
        u.setUserId(userId);
        return u;
    }

    @Test
    void onDeadLetterInsertsOneRowPerAdmin() {
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.of(role("ROLE-1", "ADMIN")));
        when(userRoleRepo.findByRoleId("ROLE-1"))
                .thenReturn(List.of(userRole("USR-1", "ROLE-1"), userRole("USR-2", "ROLE-1")));
        when(userRepo.findAllById(List.of("USR-1", "USR-2")))
                .thenReturn(List.of(user("USR-1"), user("USR-2")));

        listener.onDeadLetter(event());

        final ArgumentCaptor<CallbackNotificationEntity> cap =
                ArgumentCaptor.forClass(CallbackNotificationEntity.class);
        verify(notifRepo, times(2)).save(cap.capture());
        assertThat(cap.getAllValues()).extracting(CallbackNotificationEntity::getUserId)
                .containsExactlyInAnyOrder("USR-1", "USR-2");
        assertThat(cap.getAllValues()).extracting(CallbackNotificationEntity::getCategory)
                .allMatch("CALLBACK_DLQ"::equals);
        assertThat(cap.getAllValues()).extracting(CallbackNotificationEntity::getRefId)
                .allMatch("Q1"::equals);
        assertThat(cap.getAllValues()).extracting(CallbackNotificationEntity::getRefType)
                .allMatch("CALLBACK_DLQ_ENTRY"::equals);
    }

    @Test
    void onDeadLetterWithNoAdminRoleSkipsSilently() {
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.empty());

        listener.onDeadLetter(event());

        verifyNoInteractions(notifRepo);
        verifyNoInteractions(userRoleRepo);
        verifyNoInteractions(userRepo);
    }

    @Test
    void onDeadLetterWithNoAdminUsersSkipsSilently() {
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.of(role("ROLE-1", "ADMIN")));
        when(userRoleRepo.findByRoleId("ROLE-1")).thenReturn(List.of());

        listener.onDeadLetter(event());

        verifyNoInteractions(notifRepo);
        verifyNoInteractions(userRepo);
    }
}
