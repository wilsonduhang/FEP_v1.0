package com.puchain.fep.web.collector.service;

import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.collector.CollectionRunEntity;
import com.puchain.fep.web.collector.CollectionRunRepository;
import com.puchain.fep.web.collector.dto.CollectionRunQueryRequest;
import com.puchain.fep.web.collector.dto.CollectionRunResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Read-side query service for {@link CollectionRunEntity} (P4 T6b).
 *
 * <p>Encapsulates the {@link Specification} chain that the
 * {@link com.puchain.fep.web.collector.controller.CollectionRunController}
 * uses to filter the {@code GET /api/v1/collector/runs} endpoint, and adapts
 * {@link com.puchain.fep.common.domain.PageQuery}'s 1-based {@code pageNum}
 * into Spring Data's 0-based {@link Pageable} (red line
 * {@code feedback_pagination_adapter} — adapter is inline per project
 * convention, not added to {@code PageQuery} to preserve the fep-common
 * contract).</p>
 *
 * <p><b>ArchUnit compliance</b>: this service is the indirection layer
 * required by {@code controllers_must_not_directly_depend_on_repositories}
 * (see {@code com.puchain.fep.architecture.ClassDesignTest}).</p>
 *
 * <p><b>Sort</b>: results are ordered by {@code startedAt} DESC so the
 * management UI sees newest runs first.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class CollectionRunQueryService {

    /** JPA attribute name for {@link CollectionRunEntity#getStartedAt()}. */
    private static final String FIELD_STARTED_AT = "startedAt";

    /** JPA attribute name for {@link CollectionRunEntity#getAdapterId()}. */
    private static final String FIELD_ADAPTER_ID = "adapterId";

    /** JPA attribute name for {@link CollectionRunEntity#getStatus()}. */
    private static final String FIELD_STATUS = "status";

    /** Default sort: most-recent runs first. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, FIELD_STARTED_AT);

    private final CollectionRunRepository repository;

    /**
     * Constructs the query service.
     *
     * @param repository JPA repository for {@link CollectionRunEntity}, non-null
     */
    public CollectionRunQueryService(final CollectionRunRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Pages collection runs filtered by the optional fields on {@code req}.
     *
     * <p>Pagination contract: {@code pageNum} is 1-based (project convention,
     * see {@link com.puchain.fep.common.domain.PageQuery}); this method
     * translates to 0-based {@link PageRequest#of(int, int, Sort)} inline
     * (mirrors {@code ReconciliationQueryService.search} line ~99).</p>
     *
     * @param req query request, non-null; null-safe filters
     * @return paged response wrapping {@link CollectionRunResponse} content
     */
    @Transactional(readOnly = true)
    public PageResult<CollectionRunResponse> search(final CollectionRunQueryRequest req) {
        Objects.requireNonNull(req, "req");
        final Pageable pageable = PageRequest.of(
                req.getPageNum() - 1,
                req.getPageSize(),
                DEFAULT_SORT);
        final Specification<CollectionRunEntity> spec = buildSpecification(req);
        final Page<CollectionRunEntity> page = repository.findAll(spec, pageable);
        final List<CollectionRunResponse> records = page.getContent().stream()
                .map(CollectionRunResponse::from)
                .toList();
        return new PageResult<>(records, page.getTotalElements(),
                req.getPageNum(), req.getPageSize());
    }

    /**
     * Composes a {@link Specification} chain for the optional filters.
     *
     * <p>Returns {@code Specification.where(null)} when no filters apply —
     * still a valid Specification (Spring Data treats null Predicate inside
     * as "no clause"); the repo call still issues a paged query, returning
     * all rows in startedAt-DESC order.</p>
     *
     * @param req query request, non-null
     * @return composed specification (never null)
     */
    private Specification<CollectionRunEntity> buildSpecification(final CollectionRunQueryRequest req) {
        // Build a single Specification whose toPredicate combines all clauses
        // with AND. Returning a Specification chain via .and() also works but
        // would create one Specification per filter; the explicit form keeps
        // ordering and null-safety intent visible at the call site.
        return (root, query, cb) -> {
            final List<Predicate> predicates = new ArrayList<>(4);
            if (isPresent(req.getAdapterId())) {
                predicates.add(cb.equal(root.get(FIELD_ADAPTER_ID), req.getAdapterId().trim()));
            }
            if (isPresent(req.getStatus())) {
                predicates.add(cb.equal(root.get(FIELD_STATUS), req.getStatus().trim()));
            }
            if (req.getFrom() != null && req.getTo() != null) {
                predicates.add(cb.between(root.get(FIELD_STARTED_AT), req.getFrom(), req.getTo()));
            } else if (req.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(FIELD_STARTED_AT), req.getFrom()));
            } else if (req.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(FIELD_STARTED_AT), req.getTo()));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Treats {@code null} / blank as absent — protects against
     * "WHERE column = ''" matching empty strings unintentionally.
     *
     * @param s candidate string
     * @return true when present and not blank
     */
    private static boolean isPresent(final String s) {
        return s != null && !s.isBlank();
    }
}
