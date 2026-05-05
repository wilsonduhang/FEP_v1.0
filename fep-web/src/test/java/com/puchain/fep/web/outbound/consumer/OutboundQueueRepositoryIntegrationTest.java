package com.puchain.fep.web.outbound.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository behaviour for {@link OutboundQueueRepository} (P5 T2).
 *
 * <p>Verifies the {@code claimBatch(int)} native query on H2:</p>
 * <ul>
 *   <li>Returns {@code PENDING} rows and {@code RETRY} rows whose
 *       {@code next_retry_at} has elapsed</li>
 *   <li>Skips {@code RETRY} rows whose {@code next_retry_at} is still in the
 *       future</li>
 * </ul>
 *
 * <p><b>Why {@code @SpringBootTest} not {@code @DataJpaTest}:</b> the H2
 * {@code MODE=MySQL} schema is provisioned by Flyway V1-V25, which the slim
 * {@code @DataJpaTest} slice does not bootstrap reliably. Existing repository
 * tests in this module ({@code ReconciliationRecordRepositoryTest},
 * {@code JpaOutboundMessageEnqueueServiceTest}) use the same workaround.</p>
 *
 * <p>The {@code FOR UPDATE SKIP LOCKED} branch of the claim semantics (locked
 * rows skipped by concurrent pollers) cannot be exercised inside a single
 * Spring-managed transaction; that aspect is covered later by the T9
 * end-to-end consumer integration test.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@Sql("/sql/p5/outbound_queue_3_pending_2_retry.sql")
@DisplayName("OutboundQueueRepository: claimBatch PENDING + due-RETRY only")
class OutboundQueueRepositoryIntegrationTest {

    private static final String UUID_1 = "aaaa1111bbbb2222cccc3333dddd0001";
    private static final String UUID_2 = "aaaa1111bbbb2222cccc3333dddd0002";
    private static final String UUID_3 = "aaaa1111bbbb2222cccc3333dddd0003";
    private static final String UUID_4 = "aaaa1111bbbb2222cccc3333dddd0004";
    private static final String UUID_5 = "aaaa1111bbbb2222cccc3333dddd0005";
    private static final String UUID_99 = "aaaa1111bbbb2222cccc3333dddd0099";

    @Autowired
    private OutboundQueueRepository repo;

    @Test
    void claimBatch_should_return_pending_or_due_retry() {
        final List<String> claimed = repo.claimBatch(10);

        assertThat(claimed).containsExactlyInAnyOrder(UUID_1, UUID_2, UUID_3, UUID_4, UUID_5);
    }

    @Test
    void claimBatch_should_not_return_future_retry_rows() {
        final List<String> claimed = repo.claimBatch(100);

        assertThat(claimed).doesNotContain(UUID_99);
    }
}
