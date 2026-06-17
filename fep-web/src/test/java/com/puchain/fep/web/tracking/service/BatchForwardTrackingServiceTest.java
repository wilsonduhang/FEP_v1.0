package com.puchain.fep.web.tracking.service;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.BatchForwardProcessedEvent;
import com.puchain.fep.web.integration.tracking.BatchForwardRecordEntity;
import com.puchain.fep.web.integration.tracking.BatchForwardRecordRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BatchForwardTrackingService} (§6.4.1 FR-DATA-DB-01):
 * event → entity mapping, raw status derivation, and idempotent upsert.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class BatchForwardTrackingServiceTest {

    @Mock
    private BatchForwardRecordRepository repository;

    private BatchForwardProcessedEvent event(final int total, final int success, final int failed) {
        return new BatchForwardProcessedEvent(MessageType.MSG_3009, "T0000001",
                total, success, failed, Instant.parse("2026-06-16T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:05Z"));
    }

    @Test
    void track_newEvent_mapsAllFieldsAndDerivesCompletedStatus() {
        when(repository.findBySerialNo("T0000001")).thenReturn(Optional.empty());
        final BatchForwardTrackingService service = new BatchForwardTrackingService(repository);

        service.track(event(10, 10, 0));

        final ArgumentCaptor<BatchForwardRecordEntity> captor =
                ArgumentCaptor.forClass(BatchForwardRecordEntity.class);
        verify(repository).save(captor.capture());
        final BatchForwardRecordEntity saved = captor.getValue();
        assertThat(saved.getBatchForwardId()).isEqualTo("T0000001");
        assertThat(saved.getSerialNo()).isEqualTo("T0000001");
        assertThat(saved.getBatchType()).isEqualTo("3009");
        assertThat(saved.getTotalRecordCount()).isEqualTo(10);
        assertThat(saved.getSuccessRecordCount()).isEqualTo(10);
        assertThat(saved.getBatchStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getErrorLogPath()).isNull();
        assertThat(saved.getProcessStartTime()).isNotNull();
        assertThat(saved.getProcessEndTime()).isNotNull();
    }

    @Test
    void track_withFailures_derivesFailedStatus() {
        when(repository.findBySerialNo("T0000001")).thenReturn(Optional.empty());
        final BatchForwardTrackingService service = new BatchForwardTrackingService(repository);

        service.track(event(10, 8, 2));

        final ArgumentCaptor<BatchForwardRecordEntity> captor =
                ArgumentCaptor.forClass(BatchForwardRecordEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBatchStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getSuccessRecordCount()).isEqualTo(8);
    }

    @Test
    void track_existingSerial_updatesInPlaceKeepingId() {
        final BatchForwardRecordEntity existing = new BatchForwardRecordEntity();
        existing.setBatchForwardId("EXISTING-ID");
        existing.setSerialNo("T0000001");
        when(repository.findBySerialNo("T0000001")).thenReturn(Optional.of(existing));
        final BatchForwardTrackingService service = new BatchForwardTrackingService(repository);

        service.track(event(5, 5, 0));

        final ArgumentCaptor<BatchForwardRecordEntity> captor =
                ArgumentCaptor.forClass(BatchForwardRecordEntity.class);
        verify(repository).save(captor.capture());
        // existing id preserved (no new id derived on update)
        assertThat(captor.getValue().getBatchForwardId()).isEqualTo("EXISTING-ID");
        assertThat(captor.getValue().getTotalRecordCount()).isEqualTo(5);
    }
}
