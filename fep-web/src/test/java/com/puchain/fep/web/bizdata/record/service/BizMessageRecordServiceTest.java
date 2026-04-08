package com.puchain.fep.web.bizdata.record.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.bizdata.definition.domain.BizMessageDefinition;
import com.puchain.fep.web.bizdata.definition.repository.BizMessageDefinitionRepository;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.domain.BizMessageRecord;
import com.puchain.fep.web.bizdata.record.domain.EntryMethod;
import com.puchain.fep.web.bizdata.record.domain.MessageProcessStatus;
import com.puchain.fep.web.bizdata.record.dto.RecordCreateRequest;
import com.puchain.fep.web.bizdata.record.dto.RecordResponse;
import com.puchain.fep.web.bizdata.record.dto.RecordSummaryItem;
import com.puchain.fep.web.bizdata.record.repository.BizMessageRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BizMessageRecordService unit tests.
 */
@ExtendWith(MockitoExtension.class)
class BizMessageRecordServiceTest {

    @Mock
    private BizMessageRecordRepository recordRepository;

    @Mock
    private BizMessageDefinitionRepository definitionRepository;

    @InjectMocks
    private BizMessageRecordService recordService;

    @Test
    void create_withValidRequest_shouldSetPendingStatusAndManualEntry() {
        RecordCreateRequest request = new RecordCreateRequest();
        request.setMessageCode("3000");
        request.setSerialNo("SN-20260408-00001");
        request.setDirection(MessageDirection.OUTBOUND);
        request.setAmount(new BigDecimal("1000000.00"));

        when(recordRepository.existsBySerialNo("SN-20260408-00001"))
                .thenReturn(false);
        when(recordRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        RecordResponse resp = recordService.create(request);

        assertThat(resp.getProcessStatus())
                .isEqualTo(MessageProcessStatus.PENDING);
        assertThat(resp.getEntryMethod()).isEqualTo(EntryMethod.MANUAL);
        assertThat(resp.getAmount())
                .isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(resp.getMessageCode()).isEqualTo("3000");
        assertThat(resp.getSerialNo()).isEqualTo("SN-20260408-00001");
        verify(recordRepository).save(any());
    }

    @Test
    void create_withDuplicateSerialNo_shouldThrowBiz5002() {
        RecordCreateRequest request = new RecordCreateRequest();
        request.setMessageCode("3000");
        request.setSerialNo("SN-DUP");
        request.setDirection(MessageDirection.OUTBOUND);

        when(recordRepository.existsBySerialNo("SN-DUP")).thenReturn(true);

        assertThatThrownBy(() -> recordService.create(request))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(
                        ((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5002));
    }

    @Test
    void getById_withNonExistent_shouldThrowBiz5001() {
        when(recordRepository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.getById("missing"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(
                        ((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5001));
    }

    @Test
    void getSummary_withNoRecords_shouldReturnEmptyList() {
        when(recordRepository.getMessageSummary())
                .thenReturn(Collections.emptyList());

        List<RecordSummaryItem> result = recordService.getSummary();

        assertThat(result).isEmpty();
    }

    @Test
    void getSummary_withRecords_shouldAggregateByMessageCode() {
        Object[] row = {"3000", 50L, 30L, 15L, 5L};
        List<Object[]> rows = Collections.singletonList(row);
        when(recordRepository.getMessageSummary()).thenReturn(rows);

        BizMessageDefinition def = new BizMessageDefinition();
        def.setMessageCode("3000");
        def.setMessageName("电子凭证信息登记");
        when(definitionRepository.findByMessageCodeIn(List.of("3000")))
                .thenReturn(List.of(def));

        List<RecordSummaryItem> result = recordService.getSummary();

        assertThat(result).hasSize(1);
        RecordSummaryItem item = result.get(0);
        assertThat(item.getMessageCode()).isEqualTo("3000");
        assertThat(item.getMessageName()).isEqualTo("电子凭证信息登记");
        assertThat(item.getTotalCount()).isEqualTo(50L);
        assertThat(item.getSuccessCount()).isEqualTo(30L);
        assertThat(item.getPendingCount()).isEqualTo(15L);
        assertThat(item.getFailedCount()).isEqualTo(5L);
    }

    @Test
    void resubmit_withFailedRecord_shouldResetToPending() {
        BizMessageRecord entity = buildEntity("rec1", "3000",
                MessageProcessStatus.FAILED);
        entity.setErrorMessage("Previous error");
        when(recordRepository.findById("rec1"))
                .thenReturn(Optional.of(entity));
        when(recordRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        RecordResponse resp = recordService.resubmit("rec1");

        assertThat(resp.getProcessStatus())
                .isEqualTo(MessageProcessStatus.PENDING);
        assertThat(resp.getErrorMessage()).isNull();
        verify(recordRepository).save(any());
    }

    @Test
    void resubmit_withSuccessRecord_shouldThrowBiz5003() {
        BizMessageRecord entity = buildEntity("rec1", "3000",
                MessageProcessStatus.SUCCESS);
        when(recordRepository.findById("rec1"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> recordService.resubmit("rec1"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(
                        ((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5003));
    }

    private BizMessageRecord buildEntity(final String id,
                                          final String code,
                                          final MessageProcessStatus status) {
        BizMessageRecord entity = new BizMessageRecord();
        entity.setRecordId(id);
        entity.setMessageCode(code);
        entity.setSerialNo("SN-" + id);
        entity.setDirection(MessageDirection.OUTBOUND);
        entity.setProcessStatus(status);
        entity.setEntryMethod(EntryMethod.API);
        entity.setAccessCount(0);
        return entity;
    }
}
