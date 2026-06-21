package com.puchain.fep.common.domain;

import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 分页请求构造助手：把调用方 1-based 页码归一为 Spring Data 0-based {@link Pageable}。
 *
 * <p>消除 {@code PageRequest.of(pageNum - 1, pageSize[, sort])} 偏移样板在各 service 的重复，
 * 与既有内联写法字节级等价（不做 clamping、不注入默认排序）。需要边界钳制（如
 * {@code Math.max(1, pageNum)}）的站点不属本助手范围，自行处理后传入。</p>
 *
 * <p>输出侧分页响应构造见 {@link PageResult#from(org.springframework.data.domain.Page, int, int)}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class PaginationHelper {

    private PaginationHelper() {
    }

    /**
     * 构造无排序分页请求。
     *
     * @param pageNum  当前页码（1-based，调用方语义）
     * @param pageSize 每页大小
     * @return 0-based {@link Pageable}（等同 {@code PageRequest.of(pageNum - 1, pageSize)}）
     */
    public static Pageable pageable(final int pageNum, final int pageSize) {
        return PageRequest.of(pageNum - 1, pageSize);
    }

    /**
     * 构造带排序分页请求。
     *
     * @param pageNum  当前页码（1-based，调用方语义）
     * @param pageSize 每页大小
     * @param sort     排序规则（非 null，镜像 {@link PageRequest#of(int, int, Sort)} 契约）
     * @return 0-based {@link Pageable}（等同 {@code PageRequest.of(pageNum - 1, pageSize, sort)}）
     */
    public static Pageable pageable(final int pageNum, final int pageSize, final Sort sort) {
        Objects.requireNonNull(sort, "sort");
        return PageRequest.of(pageNum - 1, pageSize, sort);
    }

    /**
     * 构造带边界钳制的分页请求（无 pageSize 上限）。
     *
     * <p>{@code pageNum} 钳为 ≥1，{@code pageSize} 钳为 ≥1，再归一为 0-based
     * {@link Pageable}。与既有内联 {@code Math.max(1,pageNum)-1 / Math.max(1,pageSize)}
     * 写法字节级等价。需 pageSize 上限的站点用
     * {@link #safePageable(int, int, int, Sort)} 重载。</p>
     *
     * @param pageNum  当前页码（1-based；&lt;1 归一为 1）
     * @param pageSize 每页大小（&lt;1 归一为 1；无上限）
     * @param sort     排序规则（非 null）
     * @return 0-based 钳制后的 {@link Pageable}
     */
    public static Pageable safePageable(final int pageNum, final int pageSize, final Sort sort) {
        Objects.requireNonNull(sort, "sort");
        final int safePage = Math.max(1, pageNum) - 1;
        final int safeSize = Math.max(1, pageSize);
        return PageRequest.of(safePage, safeSize, sort);
    }

    /**
     * 构造带边界钳制的分页请求（含 pageSize 上限）。
     *
     * <p>{@code pageNum} 钳为 ≥1，{@code pageSize} 钳为 {@code [1, maxPageSize]}，
     * 再归一为 0-based {@link Pageable}。与既有内联
     * {@code Math.min(Math.max(pageSize,1),MAX)} 写法字节级等价，防御超大 pageSize。</p>
     *
     * @param pageNum     当前页码（1-based；&lt;1 归一为 1）
     * @param pageSize    每页大小（钳制到 {@code [1, maxPageSize]}）
     * @param maxPageSize 每页大小上限（须 ≥1）
     * @param sort        排序规则（非 null）
     * @return 0-based 钳制后的 {@link Pageable}
     * @throws IllegalArgumentException 当 {@code maxPageSize < 1}
     */
    public static Pageable safePageable(final int pageNum, final int pageSize,
                                        final int maxPageSize, final Sort sort) {
        Objects.requireNonNull(sort, "sort");
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("maxPageSize must be >= 1, got " + maxPageSize);
        }
        final int safePage = Math.max(1, pageNum) - 1;
        final int safeSize = Math.min(Math.max(1, pageSize), maxPageSize);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
