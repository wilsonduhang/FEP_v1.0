package com.puchain.fep.web.auth.service;

import com.puchain.fep.web.sysmgmt.rel.domain.SysRolePermission;
import com.puchain.fep.web.sysmgmt.rel.repository.SysRolePermissionRepository;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PermissionQueryHelperTest {

    @Mock
    private SysUserRoleRepository userRoleRepository;

    @Mock
    private SysRolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private PermissionQueryHelper helper;

    @Test
    void shouldReturnDedupedSortedPermissions() {
        given(userRoleRepository.findRoleIdsByUserId("userA"))
                .willReturn(List.of("R1", "R2"));
        given(rolePermissionRepository.findByRoleIdIn(List.of("R1", "R2")))
                .willReturn(List.of(
                        new SysRolePermission("R1", "m1", "P1"),
                        new SysRolePermission("R1", "m2", "P2"),
                        new SysRolePermission("R2", "m2", "P2"),
                        new SysRolePermission("R2", "m3", "P3")
                ));

        List<String> result = helper.getPermissionCodes("userA");

        assertThat(result).containsExactly("P1", "P2", "P3");
        assertThat(result).isUnmodifiable();
    }

    @Test
    void shouldReturnEmptyListWhenNoRoles() {
        given(userRoleRepository.findRoleIdsByUserId("userB"))
                .willReturn(List.of());

        List<String> result = helper.getPermissionCodes("userB");

        assertThat(result).isEmpty();
    }
}
