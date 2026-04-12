package com.puchain.fep.processor.state;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageStateMachineTest {

    private InMemoryMessageProcessStore store;
    private MessageStateMachine machine;

    @BeforeEach
    void setUp() {
        store = new InMemoryMessageProcessStore();
        machine = new MessageStateMachine(store);
    }

    private MessageProcessRecord newReceived() {
        MessageProcessRecord r = MessageProcessRecord.initial(
                IdGenerator.uuid32(), MessageType.MSG_1001, "TX" + System.nanoTime(), Instant.now());
        return store.save(r);
    }

    @Test
    void transition_shouldAllow_RECEIVED_to_VALIDATED() {
        MessageProcessRecord r = newReceived();
        MessageProcessRecord updated = machine.transition(r.getId(), MessageProcessStatus.VALIDATED);
        assertThat(updated.getStatus()).isEqualTo(MessageProcessStatus.VALIDATED);
    }

    @Test
    void transition_shouldAllow_VALIDATED_to_PROCESSING() {
        MessageProcessRecord r = newReceived();
        machine.transition(r.getId(), MessageProcessStatus.VALIDATED);
        MessageProcessRecord updated = machine.transition(r.getId(), MessageProcessStatus.PROCESSING);
        assertThat(updated.getStatus()).isEqualTo(MessageProcessStatus.PROCESSING);
    }

    @Test
    void transition_shouldAllow_PROCESSING_to_COMPLETED() {
        MessageProcessRecord r = newReceived();
        machine.transition(r.getId(), MessageProcessStatus.VALIDATED);
        machine.transition(r.getId(), MessageProcessStatus.PROCESSING);
        MessageProcessRecord updated = machine.transition(r.getId(), MessageProcessStatus.COMPLETED);
        assertThat(updated.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
    }

    @Test
    void transition_shouldReject_RECEIVED_to_PROCESSING() {
        MessageProcessRecord r = newReceived();
        assertThatThrownBy(() -> machine.transition(r.getId(), MessageProcessStatus.PROCESSING))
                .isInstanceOf(IllegalMessageStateException.class)
                .hasMessageContaining("RECEIVED")
                .hasMessageContaining("PROCESSING");
    }

    @Test
    void transition_shouldReject_fromTerminal() {
        MessageProcessRecord r = newReceived();
        machine.transition(r.getId(), MessageProcessStatus.VALIDATED);
        machine.transition(r.getId(), MessageProcessStatus.PROCESSING);
        machine.transition(r.getId(), MessageProcessStatus.COMPLETED);
        assertThatThrownBy(() -> machine.transition(r.getId(), MessageProcessStatus.FAILED))
                .isInstanceOf(IllegalMessageStateException.class);
    }

    @Test
    void transition_shouldReject_toReceived() {
        MessageProcessRecord r = newReceived();
        machine.transition(r.getId(), MessageProcessStatus.VALIDATED);
        assertThatThrownBy(() -> machine.transition(r.getId(), MessageProcessStatus.RECEIVED))
                .isInstanceOf(IllegalMessageStateException.class);
    }

    @Test
    void failWith_shouldTransitionToFailedFromAnyNonTerminal() {
        MessageProcessRecord r = newReceived();
        MessageProcessRecord failed = machine.failWith(r.getId(), FepErrorCode.PROC_8501, "xsd invalid");
        assertThat(failed.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(failed.getErrorCode()).isEqualTo("PROC_8501");
        assertThat(failed.getErrorMessage()).isEqualTo("xsd invalid");
    }

    @Test
    void failWith_shouldRejectFromTerminal() {
        MessageProcessRecord r = newReceived();
        machine.transition(r.getId(), MessageProcessStatus.VALIDATED);
        machine.transition(r.getId(), MessageProcessStatus.PROCESSING);
        machine.transition(r.getId(), MessageProcessStatus.COMPLETED);
        assertThatThrownBy(() -> machine.failWith(r.getId(), FepErrorCode.PROC_8505, "late fail"))
                .isInstanceOf(IllegalMessageStateException.class);
    }

    @Test
    void transition_shouldThrow_whenRecordNotFound() {
        String missingId = "missing-id-0000000000000000";
        assertThatThrownBy(() -> machine.transition(missingId, MessageProcessStatus.VALIDATED))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("missing-id");
    }

    @Test
    void failWith_shouldThrow_whenRecordNotFound() {
        String missingId = "missing-id-0000000000000000";
        assertThatThrownBy(() -> machine.failWith(missingId, FepErrorCode.PROC_8501, "not found"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("missing-id");
    }

    @Test
    void failWith_shouldAllow_fromValidated() {
        MessageProcessRecord r = newReceived();
        machine.transition(r.getId(), MessageProcessStatus.VALIDATED);
        MessageProcessRecord failed = machine.failWith(r.getId(), FepErrorCode.PROC_8501, "validated fail");
        assertThat(failed.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(failed.getErrorCode()).isEqualTo("PROC_8501");
    }

    @Test
    void failWith_shouldAllow_fromProcessing() {
        MessageProcessRecord r = newReceived();
        machine.transition(r.getId(), MessageProcessStatus.VALIDATED);
        machine.transition(r.getId(), MessageProcessStatus.PROCESSING);
        MessageProcessRecord failed = machine.failWith(r.getId(), FepErrorCode.PROC_8503, "processing fail");
        assertThat(failed.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
    }
}
