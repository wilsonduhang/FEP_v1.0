package com.puchain.fep.web.callback.alert.channel;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackInAppAlertChannel} 单元测试（承接原 listener 扇出断言）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackInAppAlertChannelTest {

    @Mock SysRoleRepository roleRepo;
    @Mock SysUserRoleRepository userRoleRepo;
    @Mock SysUserRepository userRepo;
    @Mock CallbackNotificationRepository notifRepo;

    private CallbackAlertMessage msg() {
        return new CallbackAlertMessage("ERROR", "回调死信 - IF-1", "queueId=q1 msgNo=9120",
                "q1", "CALLBACK_DLQ_ENTRY", null, null);
    }

    @Test
    void supports_onlyInApp() {
        CallbackInAppAlertChannel ch =
                new CallbackInAppAlertChannel(roleRepo, userRoleRepo, userRepo, notifRepo);
        assertThat(ch.supports(NotifyMethod.IN_APP)).isTrue();
        assertThat(ch.supports(NotifyMethod.EMAIL)).isFalse();
        assertThat(ch.supports(NotifyMethod.SMS)).isFalse();
    }

    @Test
    void send_shouldPersistOneNotificationPerAdmin() {
        SysRole admin = mock(SysRole.class);
        when(admin.getRoleId()).thenReturn("r-admin");
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.of(admin));
        SysUserRole ur = mock(SysUserRole.class);
        when(ur.getUserId()).thenReturn("u1");
        when(userRoleRepo.findByRoleId("r-admin")).thenReturn(List.of(ur));
        SysUser u1 = mock(SysUser.class);
        when(u1.getUserId()).thenReturn("u1");
        when(userRepo.findAllById(List.of("u1"))).thenReturn(List.of(u1));

        new CallbackInAppAlertChannel(roleRepo, userRoleRepo, userRepo, notifRepo).send(msg());

        ArgumentCaptor<CallbackNotificationEntity> cap =
                ArgumentCaptor.forClass(CallbackNotificationEntity.class);
        verify(notifRepo).save(cap.capture());
        assertThat(cap.getValue().getUserId()).isEqualTo("u1");
        assertThat(cap.getValue().getCategory()).isEqualTo("CALLBACK_DLQ");
        assertThat(cap.getValue().getRefId()).isEqualTo("q1");
    }

    @Test
    void send_shouldWarnSafelyWhenNoAdminRole() {
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.empty());
        new CallbackInAppAlertChannel(roleRepo, userRoleRepo, userRepo, notifRepo).send(msg());
        verify(notifRepo, never()).save(any());
    }
}
