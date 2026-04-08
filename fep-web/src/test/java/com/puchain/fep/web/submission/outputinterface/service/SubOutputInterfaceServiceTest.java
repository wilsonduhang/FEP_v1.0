package com.puchain.fep.web.submission.outputinterface.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.dto.OutputInterfaceCreateRequest;
import com.puchain.fep.web.submission.outputinterface.dto.OutputInterfaceResponse;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SubOutputInterfaceService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SubOutputInterfaceServiceTest {

    @Mock
    private SubOutputInterfaceRepository outputInterfaceRepository;

    @Mock
    private SysBusinessTypeRepository businessTypeRepository;

    @InjectMocks
    private SubOutputInterfaceService service;

    private OutputInterfaceCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new OutputInterfaceCreateRequest();
        validRequest.setInterfaceName("测试接口");
        validRequest.setInterfaceUrl("https://example.com/api");
        validRequest.setAuthType(InterfaceAuthType.TOKEN);
        validRequest.setTimeoutSeconds(30);
        validRequest.setRetryCount(3);
    }

    @Test
    void create_shouldSucceed_whenNameIsUnique() {
        when(outputInterfaceRepository.existsByInterfaceName("测试接口")).thenReturn(false);
        when(outputInterfaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OutputInterfaceResponse resp = service.create(validRequest);

        assertThat(resp.getInterfaceName()).isEqualTo("测试接口");
        assertThat(resp.getInterfaceUrl()).isEqualTo("https://example.com/api");
        assertThat(resp.getAuthType()).isEqualTo(InterfaceAuthType.TOKEN);
        assertThat(resp.getInterfaceStatus()).isEqualTo(EnableDisableStatus.ENABLED);
        assertThat(resp.getCallCount()).isZero();
        verify(outputInterfaceRepository).save(any());
    }

    @Test
    void create_shouldThrow_whenNameDuplicate() {
        when(outputInterfaceRepository.existsByInterfaceName("测试接口")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validRequest))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5002));
    }

    @Test
    void create_shouldThrow_whenBusinessTypeNotExists() {
        validRequest.setBusinessTypeId("nonexistent");
        when(outputInterfaceRepository.existsByInterfaceName("测试接口")).thenReturn(false);
        when(businessTypeRepository.existsById("nonexistent")).thenReturn(false);

        assertThatThrownBy(() -> service.create(validRequest))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5009));
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(outputInterfaceRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5001));
    }

    @Test
    void toggleStatus_shouldSwitchEnabled() {
        SubOutputInterface entity = new SubOutputInterface();
        entity.setInterfaceId("id1");
        entity.setInterfaceName("test");
        entity.setInterfaceUrl("https://example.com");
        entity.setAuthType(InterfaceAuthType.NONE);
        entity.setInterfaceStatus(EnableDisableStatus.ENABLED);
        entity.setCallCount(0L);
        entity.setTimeoutSeconds(30);
        entity.setRetryCount(3);
        when(outputInterfaceRepository.findById("id1")).thenReturn(Optional.of(entity));
        when(outputInterfaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OutputInterfaceResponse resp = service.toggleStatus("id1");
        assertThat(resp.getInterfaceStatus()).isEqualTo(EnableDisableStatus.DISABLED);
    }

    @Test
    void search_shouldReturnPageResult() {
        SubOutputInterface entity = new SubOutputInterface();
        entity.setInterfaceId("id1");
        entity.setInterfaceName("搜索接口");
        entity.setInterfaceUrl("https://example.com");
        entity.setAuthType(InterfaceAuthType.NONE);
        entity.setInterfaceStatus(EnableDisableStatus.ENABLED);
        entity.setCallCount(5L);
        entity.setTimeoutSeconds(30);
        entity.setRetryCount(3);
        when(outputInterfaceRepository.search(eq("搜索"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        PageResult<OutputInterfaceResponse> result = service.search("搜索", 1, 10);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getInterfaceName()).isEqualTo("搜索接口");
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(outputInterfaceRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5001));
    }

    @Test
    void update_shouldThrow_whenNameConflict() {
        SubOutputInterface entity = new SubOutputInterface();
        entity.setInterfaceId("id1");
        entity.setInterfaceName("旧名称");
        entity.setInterfaceUrl("https://example.com");
        entity.setAuthType(InterfaceAuthType.NONE);
        entity.setInterfaceStatus(EnableDisableStatus.ENABLED);
        when(outputInterfaceRepository.findById("id1")).thenReturn(Optional.of(entity));
        when(outputInterfaceRepository.existsByInterfaceNameAndIdNot("测试接口", "id1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update("id1", validRequest))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5002));
    }
}
