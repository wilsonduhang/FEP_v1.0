package com.puchain.fep.web.bizdata.definition.service;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.definition.domain.BizMessageDefinition;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionCreateRequest;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionResponse;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionUpdateRequest;
import com.puchain.fep.web.bizdata.definition.repository.BizMessageDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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
 * BizMessageDefinitionService unit tests.
 */
@ExtendWith(MockitoExtension.class)
class BizMessageDefinitionServiceTest {

    @Mock
    private BizMessageDefinitionRepository definitionRepository;

    @InjectMocks
    private BizMessageDefinitionService definitionService;

    @Test
    void create_withValidRequest_shouldSetEnabledStatus() {
        DefinitionCreateRequest request = new DefinitionCreateRequest();
        request.setMessageCode("3000");
        request.setMessageName("电子凭证信息登记");
        request.setDirection(MessageDirection.OUTBOUND);
        request.setFieldCount(33);

        when(definitionRepository.existsByMessageCode("3000")).thenReturn(false);
        when(definitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefinitionResponse resp = definitionService.create(request);

        assertThat(resp.getMessageCode()).isEqualTo("3000");
        assertThat(resp.getMessageName()).isEqualTo("电子凭证信息登记");
        assertThat(resp.getDirection()).isEqualTo(MessageDirection.OUTBOUND);
        assertThat(resp.getFieldCount()).isEqualTo(33);
        assertThat(resp.getDefinitionStatus()).isEqualTo(EnableDisableStatus.ENABLED);
        verify(definitionRepository).save(any());
    }

    @Test
    void create_withDuplicateCode_shouldThrowBiz5011() {
        DefinitionCreateRequest request = new DefinitionCreateRequest();
        request.setMessageCode("3000");
        request.setMessageName("电子凭证信息登记");
        request.setDirection(MessageDirection.OUTBOUND);

        when(definitionRepository.existsByMessageCode("3000")).thenReturn(true);

        assertThatThrownBy(() -> definitionService.create(request))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5011));
    }

    @Test
    void getById_withNonExistent_shouldThrowBiz5012() {
        when(definitionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> definitionService.getById("missing"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5012));
    }

    @Test
    void create_withMessageCode3101_shouldStore() {
        DefinitionCreateRequest request = new DefinitionCreateRequest();
        request.setMessageCode("3101");
        request.setMessageName("电子合同信息流转报文");
        request.setDirection(MessageDirection.OUTBOUND);
        request.setFieldCount(15);

        when(definitionRepository.existsByMessageCode("3101")).thenReturn(false);
        when(definitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefinitionResponse resp = definitionService.create(request);

        assertThat(resp.getMessageCode()).isEqualTo("3101");
        assertThat(resp.getMessageName()).isEqualTo("电子合同信息流转报文");
        assertThat(resp.getFieldCount()).isEqualTo(15);
        assertThat(resp.getDefinitionStatus()).isEqualTo(EnableDisableStatus.ENABLED);
        verify(definitionRepository).save(any());
    }

    @Test
    void update_withValidRequest_shouldUpdateFields() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "旧名称", EnableDisableStatus.ENABLED);
        when(definitionRepository.findById("id1")).thenReturn(Optional.of(entity));
        when(definitionRepository.existsByMessageCodeAndDefinitionIdNot("3001", "id1"))
                .thenReturn(false);
        when(definitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefinitionUpdateRequest request = new DefinitionUpdateRequest();
        request.setMessageCode("3001");
        request.setMessageName("新名称");
        request.setDirection(MessageDirection.INBOUND);
        request.setFieldCount(10);
        request.setFieldSummary("summary");
        request.setSampleXml("<xml/>");
        request.setSortOrder(5);

        DefinitionResponse resp = definitionService.update("id1", request);

        assertThat(resp.getMessageCode()).isEqualTo("3001");
        assertThat(resp.getMessageName()).isEqualTo("新名称");
        assertThat(resp.getDirection()).isEqualTo(MessageDirection.INBOUND);
        assertThat(resp.getFieldCount()).isEqualTo(10);
    }

    @Test
    void update_withDuplicateCode_shouldThrowBiz5011() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "旧名称", EnableDisableStatus.ENABLED);
        when(definitionRepository.findById("id1")).thenReturn(Optional.of(entity));
        when(definitionRepository.existsByMessageCodeAndDefinitionIdNot("3001", "id1"))
                .thenReturn(true);

        DefinitionUpdateRequest request = new DefinitionUpdateRequest();
        request.setMessageCode("3001");

        assertThatThrownBy(() -> definitionService.update("id1", request))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5011));
    }

    @Test
    void update_withNonExistent_shouldThrowBiz5012() {
        when(definitionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> definitionService.update("missing",
                new DefinitionUpdateRequest()))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5012));
    }

    @Test
    void update_withNullFields_shouldNotOverwrite() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "旧名称", EnableDisableStatus.ENABLED);
        entity.setDirection(MessageDirection.OUTBOUND);
        when(definitionRepository.findById("id1")).thenReturn(Optional.of(entity));
        when(definitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefinitionUpdateRequest request = new DefinitionUpdateRequest();
        // all fields null — should not overwrite

        DefinitionResponse resp = definitionService.update("id1", request);

        assertThat(resp.getMessageCode()).isEqualTo("3000");
        assertThat(resp.getMessageName()).isEqualTo("旧名称");
        assertThat(resp.getDirection()).isEqualTo(MessageDirection.OUTBOUND);
    }

    @Test
    void toggleStatus_fromEnabled_shouldBecomeDisabled() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "测试", EnableDisableStatus.ENABLED);
        when(definitionRepository.findById("id1")).thenReturn(Optional.of(entity));
        when(definitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefinitionResponse resp = definitionService.toggleStatus("id1");

        assertThat(resp.getDefinitionStatus()).isEqualTo(EnableDisableStatus.DISABLED);
    }

    @Test
    void toggleStatus_fromDisabled_shouldBecomeEnabled() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "测试", EnableDisableStatus.DISABLED);
        when(definitionRepository.findById("id1")).thenReturn(Optional.of(entity));
        when(definitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DefinitionResponse resp = definitionService.toggleStatus("id1");

        assertThat(resp.getDefinitionStatus()).isEqualTo(EnableDisableStatus.ENABLED);
    }

    @Test
    void toggleStatus_withNonExistent_shouldThrowBiz5012() {
        when(definitionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> definitionService.toggleStatus("missing"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5012));
    }

    @Test
    void delete_shouldSucceed_whenExists() {
        when(definitionRepository.existsById("id1")).thenReturn(true);

        definitionService.delete("id1");

        verify(definitionRepository).deleteById("id1");
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(definitionRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> definitionService.delete("missing"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5012));
    }

    @Test
    void search_shouldReturnPagedResults() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "电子凭证", EnableDisableStatus.ENABLED);
        Page<BizMessageDefinition> page = new PageImpl<>(List.of(entity));
        when(definitionRepository.search(eq("电子"), eq(null), eq(null),
                eq(null), any(Pageable.class))).thenReturn(page);

        PageResult<DefinitionResponse> result =
                definitionService.search("电子", null, null, null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getMessageCode()).isEqualTo("3000");
        assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    void search_allFiltersNull_shouldReturnAll() {
        BizMessageDefinition e1 = buildEntity("id1", "3000",
                "电子凭证", EnableDisableStatus.ENABLED);
        BizMessageDefinition e2 = buildEntity("id2", "1001",
                "企业信息查询", EnableDisableStatus.DISABLED);
        Page<BizMessageDefinition> page = new PageImpl<>(List.of(e1, e2));
        when(definitionRepository.search(eq(null), eq(null), eq(null),
                eq(null), any(Pageable.class))).thenReturn(page);

        PageResult<DefinitionResponse> result =
                definitionService.search(null, null, null, null, 1, 10);

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2);
    }

    @Test
    void search_byMessageCode_shouldFilterExact() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "电子凭证", EnableDisableStatus.ENABLED);
        Page<BizMessageDefinition> page = new PageImpl<>(List.of(entity));
        when(definitionRepository.search(eq(null), eq("3000"), eq(null),
                eq(null), any(Pageable.class))).thenReturn(page);

        PageResult<DefinitionResponse> result =
                definitionService.search(null, "3000", null, null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getMessageCode()).isEqualTo("3000");
    }

    @Test
    void search_byDirection_shouldFilterEnum() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "电子凭证", EnableDisableStatus.ENABLED);
        entity.setDirection(MessageDirection.INBOUND);
        Page<BizMessageDefinition> page = new PageImpl<>(List.of(entity));
        when(definitionRepository.search(eq(null), eq(null),
                eq(MessageDirection.INBOUND), eq(null),
                any(Pageable.class))).thenReturn(page);

        PageResult<DefinitionResponse> result =
                definitionService.search(null, null, MessageDirection.INBOUND,
                        null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getDirection())
                .isEqualTo(MessageDirection.INBOUND);
    }

    @Test
    void search_byDirectionAndStatus_shouldApplyAndLogic() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "电子凭证", EnableDisableStatus.DISABLED);
        entity.setDirection(MessageDirection.OUTBOUND);
        Page<BizMessageDefinition> page = new PageImpl<>(List.of(entity));
        when(definitionRepository.search(eq(null), eq(null),
                eq(MessageDirection.OUTBOUND),
                eq(EnableDisableStatus.DISABLED),
                any(Pageable.class))).thenReturn(page);

        PageResult<DefinitionResponse> result =
                definitionService.search(null, null, MessageDirection.OUTBOUND,
                        EnableDisableStatus.DISABLED, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getDirection())
                .isEqualTo(MessageDirection.OUTBOUND);
        assertThat(result.getRecords().get(0).getDefinitionStatus())
                .isEqualTo(EnableDisableStatus.DISABLED);
    }

    @Test
    void getById_shouldReturnResponse_whenExists() {
        BizMessageDefinition entity = buildEntity("id1", "3000",
                "电子凭证信息登记", EnableDisableStatus.ENABLED);
        entity.setFieldSummary("summary");
        entity.setSampleXml("<xml/>");
        when(definitionRepository.findById("id1")).thenReturn(Optional.of(entity));

        DefinitionResponse resp = definitionService.getById("id1");

        assertThat(resp.getDefinitionId()).isEqualTo("id1");
        assertThat(resp.getFieldSummary()).isEqualTo("summary");
        assertThat(resp.getSampleXml()).isEqualTo("<xml/>");
    }

    private BizMessageDefinition buildEntity(final String id, final String code,
                                              final String name,
                                              final EnableDisableStatus status) {
        BizMessageDefinition entity = new BizMessageDefinition();
        entity.setDefinitionId(id);
        entity.setMessageCode(code);
        entity.setMessageName(name);
        entity.setDefinitionStatus(status);
        entity.setDirection(MessageDirection.OUTBOUND);
        return entity;
    }
}
