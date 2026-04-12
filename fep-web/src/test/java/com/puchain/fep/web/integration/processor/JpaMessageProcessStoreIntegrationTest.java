package com.puchain.fep.web.integration.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.processor.state.MessageProcessStore;
import com.puchain.fep.web.config.TestRedisConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for {@link JpaMessageProcessStore}.
 *
 * <p>Boots the full Spring context so that the JPA adapter, Hibernate, H2 and
 * the V16 Flyway migration are exercised together. Verifies the adapter wins
 * over {@code InMemoryMessageProcessStore} and that all five
 * {@link MessageProcessStore} operations round-trip through the database.</p>
 *
 * <p>Named {@code *IntegrationTest} (not {@code *IT}) because this repository
 * uses Surefire for all tests and Failsafe is not configured — {@code *IT}
 * classes would be excluded from the default build.</p>
 */
@SpringBootTest
@Import(TestRedisConfiguration.class)
class JpaMessageProcessStoreIntegrationTest {

    @Autowired
    private MessageProcessStore store;

    @Test
    void store_shouldBeJpaAdapter() {
        assertThat(store).isInstanceOf(JpaMessageProcessStore.class);
    }

    @Test
    void saveAndFindById_shouldPersistToDatabase() {
        final String id = IdGenerator.uuid32();
        final MessageProcessRecord record = MessageProcessRecord.initial(
                id, MessageType.MSG_1001, "IT-TX-" + id.substring(0, 8), Instant.now());

        store.save(record);

        assertThat(store.findById(id))
                .isPresent()
                .get()
                .extracting(MessageProcessRecord::getMessageType)
                .isEqualTo(MessageType.MSG_1001);
    }

    @Test
    void updateStatus_shouldPersistFailureMetadata() {
        final String id = IdGenerator.uuid32();
        store.save(MessageProcessRecord.initial(
                id, MessageType.MSG_1001, "IT-FAIL-" + id.substring(0, 8), Instant.now()));

        store.updateStatus(id, MessageProcessStatus.FAILED, "PROC_8501", "xsd err");

        final MessageProcessRecord reloaded = store.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(reloaded.getErrorCode()).isEqualTo("PROC_8501");
        assertThat(reloaded.getErrorMessage()).isEqualTo("xsd err");
    }

    @Test
    void findByTransitionNo_shouldRoundTrip() {
        final String id = IdGenerator.uuid32();
        final String txNo = "IT-RT-" + id.substring(0, 8);
        store.save(MessageProcessRecord.initial(id, MessageType.MSG_9005, txNo, Instant.now()));

        assertThat(store.findByTransitionNo(txNo))
                .isPresent()
                .get()
                .extracting(MessageProcessRecord::getMessageType)
                .isEqualTo(MessageType.MSG_9005);
    }

    @Test
    void countByStatus_shouldReturnNonNegativeCount() {
        final long count = store.countByStatus(MessageProcessStatus.COMPLETED);
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
