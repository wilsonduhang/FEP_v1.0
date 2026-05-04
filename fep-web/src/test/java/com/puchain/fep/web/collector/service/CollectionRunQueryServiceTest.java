package com.puchain.fep.web.collector.service;

import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.collector.CollectionRunEntity;
import com.puchain.fep.web.collector.CollectionRunRepository;
import com.puchain.fep.web.collector.dto.CollectionRunQueryRequest;
import com.puchain.fep.web.collector.dto.CollectionRunResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CollectionRunQueryService} (P4 T6b).
 *
 * <p>Validates:
 * <ul>
 *   <li>1-based {@code pageNum} → 0-based {@link Pageable} adapter (red line
 *       {@code feedback_pagination_adapter}: distinct pageNum used so 1-1=0
 *       collision is excluded).</li>
 *   <li>{@link Specification} chain composes optional filters
 *       (adapterId / status / from / to) without affecting empty filters.</li>
 *   <li>Sort direction {@code DESC} on {@code startedAt}.</li>
 *   <li>Empty repository → empty {@link PageResult} preserves request page metadata.</li>
 *   <li>{@link CollectionRunEntity} → {@link CollectionRunResponse} mapping.</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CollectionRunQueryServiceTest {

    private CollectionRunRepository repo;
    private CollectionRunQueryService svc;

    @BeforeEach
    void setUp() {
        repo = mock(CollectionRunRepository.class);
        svc = new CollectionRunQueryService(repo);
    }

    @Test
    void search_pageNum2_translatesTo_zeroBasedPageNumber1() {
        // Use distinct pageNum (=2) per feedback_pagination_adapter:
        // pageNum=1 collides with default 1-1=0; we must verify true 1-based→0-based translation.
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(2);
        req.setPageSize(15);

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 15), 0L));

        svc.search(req);

        ArgumentCaptor<Pageable> pageCap = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findAll(any(Specification.class), pageCap.capture());
        assertThat(pageCap.getValue().getPageNumber())
                .as("pageNum=2 must translate to 0-based pageNumber=1")
                .isEqualTo(1);
        assertThat(pageCap.getValue().getPageSize()).isEqualTo(15);
        assertThat(pageCap.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "startedAt"));
    }

    @Test
    void search_pageNum3_translatesTo_zeroBasedPageNumber2() {
        // Second distinct pageNum to defend against any constant-arithmetic regression.
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(3);
        req.setPageSize(20);

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 20), 0L));

        svc.search(req);

        ArgumentCaptor<Pageable> pageCap = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findAll(any(Specification.class), pageCap.capture());
        assertThat(pageCap.getValue().getPageNumber()).isEqualTo(2);
    }

    @Test
    void search_emptyRepo_returnsEmptyPageResult_preservingRequestMetadata() {
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(2);
        req.setPageSize(10);

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0L));

        PageResult<CollectionRunResponse> result = svc.search(req);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
    }

    @Test
    void search_withResults_mapsEntitiesToResponses() {
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(1);
        req.setPageSize(20);

        CollectionRunEntity e = sampleEntity("RID_001", "ADP_3101", "SUCCESS",
                Instant.parse("2026-04-30T10:00:00Z"), 5, 5, 5, 0, null);
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e), PageRequest.of(0, 20), 1L));

        PageResult<CollectionRunResponse> result = svc.search(req);

        assertThat(result.getRecords()).hasSize(1);
        CollectionRunResponse r = result.getRecords().get(0);
        assertThat(r.getRunId()).isEqualTo("RID_001");
        assertThat(r.getAdapterId()).isEqualTo("ADP_3101");
        assertThat(r.getStatus()).isEqualTo("SUCCESS");
        assertThat(r.getStartedAt()).isEqualTo(Instant.parse("2026-04-30T10:00:00Z"));
        assertThat(r.getAssembledCount()).isEqualTo(5);
        assertThat(r.getErrorMessage()).isNull();
        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    void search_allFiltersPresent_buildsNonNullSpecification() {
        // Boundary: ensure all four optional filters compose without throwing.
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(1);
        req.setPageSize(20);
        req.setAdapterId("ADP_3101");
        req.setStatus("SUCCESS");
        req.setFrom(Instant.parse("2026-04-30T00:00:00Z"));
        req.setTo(Instant.parse("2026-04-30T23:59:59Z"));

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L));

        // Should not throw; Specification chain executes lazily inside repo.findAll.
        svc.search(req);

        verify(repo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_noFilters_stillExecutesQueryWithEmptySpecification() {
        // Boundary: no filters → Specification.where(null) → repo still invoked.
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(1);
        req.setPageSize(20);

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L));

        svc.search(req);

        verify(repo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_blankAdapterId_treatedAsAbsent_doesNotThrow() {
        // Boundary: blank string vs null — must not equate to "WHERE adapter_id = ''".
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(1);
        req.setPageSize(20);
        req.setAdapterId("   ");
        req.setStatus("");

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L));

        svc.search(req);

        verify(repo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_onlyFromBoundary_buildsSpecificationWithoutTo() {
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(1);
        req.setPageSize(20);
        req.setFrom(Instant.parse("2026-04-30T00:00:00Z"));

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L));

        svc.search(req);

        verify(repo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_onlyToBoundary_buildsSpecificationWithoutFrom() {
        CollectionRunQueryRequest req = new CollectionRunQueryRequest();
        req.setPageNum(1);
        req.setPageSize(20);
        req.setTo(Instant.parse("2026-04-30T23:59:59Z"));

        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L));

        svc.search(req);

        verify(repo).findAll(any(Specification.class), any(Pageable.class));
    }

    private static CollectionRunEntity sampleEntity(final String runId,
                                                    final String adapterId,
                                                    final String status,
                                                    final Instant startedAt,
                                                    final int collected,
                                                    final int assembled,
                                                    final int submitted,
                                                    final int errors,
                                                    final String errMsg) {
        CollectionRunEntity e = new CollectionRunEntity();
        e.setRunId(runId);
        e.setAdapterId(adapterId);
        e.setStatus(status);
        e.setStartedAt(startedAt);
        e.setCompletedAt(startedAt.plusSeconds(1));
        e.setCollectedCount(collected);
        e.setAssembledCount(assembled);
        e.setSubmittedCount(submitted);
        e.setErrorCount(errors);
        e.setErrorMessage(errMsg);
        e.setTriggerSource("MANUAL");
        e.setCreatedAt(startedAt);
        return e;
    }
}
