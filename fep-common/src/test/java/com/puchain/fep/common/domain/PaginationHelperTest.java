package com.puchain.fep.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * {@link PaginationHelper} 单元测试：验证 1-based→0-based 归一与既有内联
 * {@code PageRequest.of(pageNum - 1, ...)} 写法的字节级等价。
 */
class PaginationHelperTest {

    @Test
    void pageableWithoutSortConvertsOneBasedToZeroBased() {
        final Pageable pageable = PaginationHelper.pageable(1, 20);
        assertThat(pageable.getPageNumber()).isZero();        // 1-based 1 → 0-based 0
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().isSorted()).isFalse();   // 与 PageRequest.of(0,20) 一致：unsorted
    }

    @Test
    void pageableSecondPageMapsToIndexOne() {
        assertThat(PaginationHelper.pageable(2, 15).getPageNumber()).isEqualTo(1);
    }

    @Test
    void pageableWithSortPreservesSort() {
        final Sort sort = Sort.by("createTime").descending();
        final Pageable pageable = PaginationHelper.pageable(3, 10, sort);
        assertThat(pageable.getPageNumber()).isEqualTo(2);     // 3 → 2
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(sort);
    }

    @Test
    void pageableIsByteEquivalentToInlinePageRequest() {
        // 收敛不变量：helper 输出 == 既有内联写法
        assertThat(PaginationHelper.pageable(4, 25))
                .isEqualTo(PageRequest.of(3, 25));
        final Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        assertThat(PaginationHelper.pageable(4, 25, sort))
                .isEqualTo(PageRequest.of(3, 25, sort));
    }

    @Test
    void pageableWithNullSortThrowsNpe() {
        // 镜像 PageRequest.of(page,size,Sort) 对 null sort 的 NPE 契约
        assertThatThrownBy(() -> PaginationHelper.pageable(1, 10, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void safePageableClampsPageNumAndSizeLowerBound() {
        // pageNum<1 → 1（0-based 0）；pageSize<1 → 1
        final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        final Pageable p = PaginationHelper.safePageable(0, 0, sort);
        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(1);
        assertThat(p.getSort()).isEqualTo(sort);
    }

    @Test
    void safePageableNoUpperCapIsByteEquivalentToReconciliationInline() {
        // 收敛不变量：== ReconciliationQueryService 既有内联（无上限）
        final Sort sort = Sort.by(Sort.Direction.ASC, "reconciliationDate");
        assertThat(PaginationHelper.safePageable(0, 50, sort))
                .isEqualTo(PageRequest.of(Math.max(1, 0) - 1, Math.max(1, 50), sort));
        assertThat(PaginationHelper.safePageable(3, 9999, sort))   // 无上限：9999 原样
                .isEqualTo(PageRequest.of(2, 9999, sort));
    }

    @Test
    void safePageableWithMaxClampsUpperBound() {
        final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        // pageSize>max → max；max 路径下下限同样钳制
        assertThat(PaginationHelper.safePageable(1, 500, 200, sort).getPageSize()).isEqualTo(200);
        assertThat(PaginationHelper.safePageable(1, 0, 200, sort).getPageSize()).isEqualTo(1);
    }

    @Test
    void safePageableWithMaxIsByteEquivalentToMessageReviewInline() {
        // 收敛不变量：== MessageReviewTaskService 既有内联（MAX_PAGE_SIZE=200）
        final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        final int max = 200;
        assertThat(PaginationHelper.safePageable(2, 300, max, sort))
                .isEqualTo(PageRequest.of(Math.max(2, 1) - 1, Math.min(Math.max(300, 1), max), sort));
    }

    @Test
    void safePageableNullSortThrowsNpe() {
        assertThatThrownBy(() -> PaginationHelper.safePageable(1, 10, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> PaginationHelper.safePageable(1, 10, 200, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void safePageableInvalidMaxThrows() {
        final Sort sort = Sort.by("createdAt");
        assertThatThrownBy(() -> PaginationHelper.safePageable(1, 10, 0, sort))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
