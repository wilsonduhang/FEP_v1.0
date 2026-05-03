package com.puchain.fep.web.collector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CollectionRecordOffsetEntity} (P4 T8).
 *
 * <p>Production reads/writes go through {@link JpaWatermarkStore} (T8-fix —
 * replaces the prior {@code JdbcTemplate}-based MERGE which did not parse on
 * MySQL 8). The future T6b admin UI may surface an "active watermarks" panel
 * via a Spring Data finder on the same repository.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface CollectionRecordOffsetRepository
        extends JpaRepository<CollectionRecordOffsetEntity, String> {
}
