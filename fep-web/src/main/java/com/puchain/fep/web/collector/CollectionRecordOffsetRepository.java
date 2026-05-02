package com.puchain.fep.web.collector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CollectionRecordOffsetEntity} (P4 T8).
 *
 * <p>Surface kept minimal — production writes go through
 * {@link JdbcWatermarkStore} via {@code JdbcTemplate}; this repository is here
 * for management-UI reads and {@code @DataJpaTest} schema validation.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface CollectionRecordOffsetRepository
        extends JpaRepository<CollectionRecordOffsetEntity, String> {
}
