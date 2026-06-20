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
}
