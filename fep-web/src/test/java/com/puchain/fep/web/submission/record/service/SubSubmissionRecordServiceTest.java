package com.puchain.fep.web.submission.record.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.submission.record.domain.EntryMethod;
import com.puchain.fep.web.submission.record.domain.PushStatus;
import com.puchain.fep.web.submission.record.domain.SubSubmissionRecord;
import com.puchain.fep.web.submission.record.dto.MessageSummaryResponse;
import com.puchain.fep.web.submission.record.dto.SubmissionRecordResponse;
import com.puchain.fep.web.submission.record.repository.SubSubmissionRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SubSubmissionRecordService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SubSubmissionRecordServiceTest {

    @Mock
    private SubSubmissionRecordRepository recordRepository;

    @InjectMocks
    private SubSubmissionRecordService service;

    @Test
    void createManualRecord_shouldSetCorrectDefaults() {
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmissionRecordResponse resp = service.createManualRecord(
                "3101", "电子合同信息流转报文", "biz1", 10, "admin");

        assertThat(resp.getMessageType()).isEqualTo("3101");
        assertThat(resp.getMessageName()).isEqualTo("电子合同信息流转报文");
        assertThat(resp.getBusinessTypeId()).isEqualTo("biz1");
        assertThat(resp.getDataCount()).isEqualTo(10);
        assertThat(resp.getEntryMethod()).isEqualTo(EntryMethod.MANUAL_ENTRY);
        assertThat(resp.getEntryBy()).isEqualTo("admin");
        assertThat(resp.getPushStatus()).isEqualTo(PushStatus.PENDING);
        assertThat(resp.getRecordId()).isNotBlank();
        verify(recordRepository).save(any());
    }

    @Test
    void triggerPush_shouldThrow_whenNoPendingRecords() {
        when(recordRepository.findByPushStatusAndRecordIdIn(
                eq(PushStatus.PENDING), anyList()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.triggerPush(List.of("id1", "id2")))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(
                        ((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5003));
    }

    @Test
    void triggerPush_shouldUpdateToPushing() {
        SubSubmissionRecord record = new SubSubmissionRecord();
        record.setRecordId("id1");
        record.setMessageType("3101");
        record.setMessageName("电子合同信息流转报文");
        record.setEntryMethod(EntryMethod.MANUAL_ENTRY);
        record.setPushStatus(PushStatus.PENDING);
        record.setDataCount(1);

        when(recordRepository.findByPushStatusAndRecordIdIn(
                eq(PushStatus.PENDING), anyList()))
                .thenReturn(List.of(record));
        when(recordRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        List<SubmissionRecordResponse> result =
                service.triggerPush(List.of("id1"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPushStatus())
                .isEqualTo(PushStatus.PUSHING);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(recordRepository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(
                        ((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.BIZ_5001));
    }

    @Test
    void getMessageSummary_shouldMapCorrectly() {
        Object[] row = new Object[]{"3101", "电子合同信息流转报文", "biz1", 100L};
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(recordRepository.aggregateByMessageType())
                .thenReturn(rows);
        when(recordRepository.countByMessageTypeAndPushStatus(
                "3101", PushStatus.PUSHED))
                .thenReturn(80L);

        List<MessageSummaryResponse> result = service.getMessageSummary();

        assertThat(result).hasSize(1);
        MessageSummaryResponse summary = result.get(0);
        assertThat(summary.getMessageType()).isEqualTo("3101");
        assertThat(summary.getMessageName()).isEqualTo("电子合同信息流转报文");
        assertThat(summary.getBusinessTypeId()).isEqualTo("biz1");
        assertThat(summary.getTotalCount()).isEqualTo(100L);
        assertThat(summary.getPushedCount()).isEqualTo(80L);
        assertThat(summary.getPendingCount()).isEqualTo(20L);
    }
}
