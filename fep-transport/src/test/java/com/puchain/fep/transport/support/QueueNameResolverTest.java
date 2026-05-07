package com.puchain.fep.transport.support;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.transport.support.QueueNameResolver.QueueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link QueueNameResolver}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class QueueNameResolverTest {

    private static final String INSTITUTION_CODE = "B1234567890123";

    private final QueueNameResolver resolver = new QueueNameResolver(INSTITUTION_CODE);

    @ParameterizedTest
    @CsvSource({
        "REALTIME_LOCAL,   QLOCAL.B1234567890123.REAL.1",
        "BATCH_LOCAL,      QLOCAL.B1234567890123.BATCH.1",
        "REALTIME_REMOTE,  QREMOTE." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1",
        "BATCH_REMOTE,     QREMOTE." + FepConstants.HNDEMP_NODE_CODE + ".BATCH.1",
        "REALTIME_DEST,    QLOCAL." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1",
        "BATCH_DEST,       QLOCAL." + FepConstants.HNDEMP_NODE_CODE + ".BATCH.1",
        "REALTIME_SEND,    QSEND." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1",
        "BATCH_SEND,       QSEND." + FepConstants.HNDEMP_NODE_CODE + ".BATCH.1",
        "DEAD_LETTER,      QDEAD.B1234567890123"
    })
    void resolve_allQueueTypes_shouldReturnCorrectName(final QueueType queueType, final String expected) {
        assertThat(resolver.resolve(queueType)).isEqualTo(expected);
    }

    @Test
    void resolveQcu_shouldReturnCorrectFormat() {
        assertThat(resolver.resolveQcu()).isEqualTo("QCU_HNDEMP_B1234567890123_1");
    }

    @Test
    void constructor_nullInstitutionCode_shouldThrowNpe() {
        assertThatThrownBy(() -> new QueueNameResolver(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolve_nullQueueType_shouldThrowNpe() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(NullPointerException.class);
    }
}
