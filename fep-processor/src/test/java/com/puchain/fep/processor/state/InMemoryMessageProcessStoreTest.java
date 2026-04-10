package com.puchain.fep.processor.state;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryMessageProcessStoreTest {

    private InMemoryMessageProcessStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryMessageProcessStore();
    }

    @Test
    void save_shouldPersistAndReturn() {
        MessageProcessRecord record = MessageProcessRecord.initial(
                IdGenerator.uuid32(), MessageType.MSG_1001, "TX20260411001", Instant.now());
        MessageProcessRecord saved = store.save(record);
        assertThat(saved).isSameAs(record);
        assertThat(store.findById(record.getId())).contains(record);
    }

    @Test
    void save_shouldBeIdempotentOnSameId() {
        String id = IdGenerator.uuid32();
        Instant t0 = Instant.now();
        MessageProcessRecord v1 = MessageProcessRecord.initial(id, MessageType.MSG_1001, "TX1", t0);
        MessageProcessRecord v2 = v1.withStatus(MessageProcessStatus.VALIDATED, t0.plusSeconds(1));
        store.save(v1);
        store.save(v2);
        assertThat(store.findById(id)).map(MessageProcessRecord::getStatus)
                .contains(MessageProcessStatus.VALIDATED);
    }

    @Test
    void findByTransitionNo_shouldReturnEmptyWhenMissing() {
        assertThat(store.findByTransitionNo("nope")).isEmpty();
    }

    @Test
    void findByTransitionNo_shouldReturnSingleMatch() {
        MessageProcessRecord record = MessageProcessRecord.initial(
                IdGenerator.uuid32(), MessageType.MSG_1001, "TX-UNIQUE", Instant.now());
        store.save(record);
        Optional<MessageProcessRecord> found = store.findByTransitionNo("TX-UNIQUE");
        assertThat(found).isPresent().get().isEqualTo(record);
    }

    @Test
    void findByTransitionNo_shouldFailOnDuplicates() {
        Instant now = Instant.now();
        store.save(MessageProcessRecord.initial(IdGenerator.uuid32(), MessageType.MSG_1001, "DUP", now));
        store.save(MessageProcessRecord.initial(IdGenerator.uuid32(), MessageType.MSG_1001, "DUP", now));
        assertThatThrownBy(() -> store.findByTransitionNo("DUP"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DUP");
    }

    @Test
    void updateStatus_shouldTransitionAndPreserveId() {
        MessageProcessRecord record = MessageProcessRecord.initial(
                IdGenerator.uuid32(), MessageType.MSG_1001, "TX-UP", Instant.now());
        store.save(record);
        MessageProcessRecord updated = store.updateStatus(
                record.getId(), MessageProcessStatus.VALIDATED, null, null);
        assertThat(updated.getStatus()).isEqualTo(MessageProcessStatus.VALIDATED);
        assertThat(updated.getId()).isEqualTo(record.getId());
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(record.getCreatedAt());
    }

    @Test
    void updateStatus_shouldStoreErrorOnFailure() {
        MessageProcessRecord record = MessageProcessRecord.initial(
                IdGenerator.uuid32(), MessageType.MSG_1001, "TX-FAIL", Instant.now());
        store.save(record);
        MessageProcessRecord failed = store.updateStatus(
                record.getId(), MessageProcessStatus.FAILED, "PROC_8501", "xsd fail");
        assertThat(failed.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(failed.getErrorCode()).isEqualTo("PROC_8501");
        assertThat(failed.getErrorMessage()).isEqualTo("xsd fail");
    }

    @Test
    void updateStatus_shouldThrowWhenIdMissing() {
        assertThatThrownBy(() -> store.updateStatus("missing", MessageProcessStatus.VALIDATED, null, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void countByStatus_shouldReflectSaves() {
        Instant now = Instant.now();
        store.save(MessageProcessRecord.initial(IdGenerator.uuid32(), MessageType.MSG_1001, "A", now));
        store.save(MessageProcessRecord.initial(IdGenerator.uuid32(), MessageType.MSG_1001, "B", now));
        assertThat(store.countByStatus(MessageProcessStatus.RECEIVED)).isEqualTo(2);
        assertThat(store.countByStatus(MessageProcessStatus.COMPLETED)).isZero();
    }

    @Test
    void record_shouldRejectNullRequiredFields() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new MessageProcessRecord(
                null, MessageType.MSG_1001, "T1", MessageProcessStatus.RECEIVED, now, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("id");
        assertThatThrownBy(() -> new MessageProcessRecord(
                "id1", null, "T1", MessageProcessStatus.RECEIVED, now, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("messageType");
        assertThatThrownBy(() -> new MessageProcessRecord(
                "id1", MessageType.MSG_1001, "T1", null, now, now, null, null))
                .isInstanceOf(NullPointerException.class).hasMessage("status");
    }

    @Test
    void record_shouldRejectOversizedTransitionNo() {
        Instant now = Instant.now();
        String tooLong = "A".repeat(31);
        assertThatThrownBy(() -> new MessageProcessRecord(
                IdGenerator.uuid32(),
                MessageType.MSG_1001, tooLong, MessageProcessStatus.RECEIVED, now, now, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transitionNo length");
    }

    @Test
    void status_isTerminal_shouldDistinguishTerminalStates() {
        assertThat(MessageProcessStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(MessageProcessStatus.FAILED.isTerminal()).isTrue();
        assertThat(MessageProcessStatus.RECEIVED.isTerminal()).isFalse();
        assertThat(MessageProcessStatus.VALIDATED.isTerminal()).isFalse();
        assertThat(MessageProcessStatus.PROCESSING.isTerminal()).isFalse();
    }
}
