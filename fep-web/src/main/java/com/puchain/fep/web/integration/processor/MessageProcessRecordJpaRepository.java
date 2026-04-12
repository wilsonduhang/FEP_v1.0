package com.puchain.fep.web.integration.processor;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link MessageProcessRecordEntity}.
 *
 * <p>Exposes the minimum query surface required by
 * {@link JpaMessageProcessStore}:</p>
 * <ul>
 *   <li>CRUD via {@link JpaRepository}.</li>
 *   <li>{@link #findAllByTransitionNo(String)} to honour the port contract of
 *       detecting duplicate transition numbers (data anomaly fast-fail).</li>
 *   <li>{@link #countByStatus(String)} for monitoring metrics.</li>
 * </ul>
 */
@Repository
public interface MessageProcessRecordJpaRepository
        extends JpaRepository<MessageProcessRecordEntity, String> {

    /**
     * Returns all records whose {@code transitionNo} equals the argument.
     * Under normal operation the unique constraint guarantees at most one row;
     * the adapter uses the list form to detect and reject anomalies.
     *
     * @param transitionNo business transition number
     * @return matching rows, possibly empty
     */
    List<MessageProcessRecordEntity> findAllByTransitionNo(String transitionNo);

    /**
     * Counts records with the given status string.
     *
     * @param status {@code MessageProcessStatus.name()}
     * @return non-negative count
     */
    long countByStatus(String status);
}
