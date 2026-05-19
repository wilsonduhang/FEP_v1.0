package com.puchain.fep.web.callback.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeMsgNoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackTargetResolverTest {

    @Mock
    private SysBusinessTypeMsgNoRepository msgNoRepo;
    @Mock
    private SubOutputInterfaceRepository interfaceRepo;

    @Test
    void resolve_noConfig_shouldReturnEmptyNotThrow() {
        when(msgNoRepo.findBusinessTypeIdsByMsgNo("9999")).thenReturn(List.of());
        var resolver = new CallbackTargetResolver(msgNoRepo, interfaceRepo);
        assertThat(resolver.resolve("9999")).isEmpty();
    }

    @Test
    void resolve_fanOut_shouldReturnAllEnabledInterfaces() {
        SubOutputInterface i1 = new SubOutputInterface();
        SubOutputInterface i2 = new SubOutputInterface();
        when(msgNoRepo.findBusinessTypeIdsByMsgNo("2103"))
                .thenReturn(List.of("bt-1", "bt-2"));
        when(interfaceRepo.findByBusinessTypeIdInAndInterfaceStatus(
                List.of("bt-1", "bt-2"),
                EnableDisableStatus.ENABLED))
                .thenReturn(List.of(i1, i2));
        var resolver = new CallbackTargetResolver(msgNoRepo, interfaceRepo);
        assertThat(resolver.resolve("2103")).containsExactly(i1, i2);
    }
}
