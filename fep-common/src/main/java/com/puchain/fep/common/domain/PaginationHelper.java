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
}
