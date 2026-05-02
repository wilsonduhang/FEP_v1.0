package com.puchain.fep.web.collector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CollectionRunEntity} (P4 T8).
 *
 * <p>Combines basic CRUD ({@link JpaRepository}) with dynamic-criteria search
 * ({@link JpaSpecificationExecutor}) so the future T6b
 * {@code CollectionRunController#search(adapterId, status, from, to, Pageable)}
 * can compose optional filters without proliferating finder methods. This is the
 * project's first use of {@code Specification}; rationale lives in Plan §T8 §2.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface CollectionRunRepository
        extends JpaRepository<CollectionRunEntity, String>,
                JpaSpecificationExecutor<CollectionRunEntity> {
}
