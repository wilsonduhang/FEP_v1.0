package com.puchain.fep.common.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * 分页响应结构。
 *
 * <p>参见 PRD v1.3 §9.2 列表与分页规范。</p>
 *
 * @param <T> 记录类型
 * @author FEP Team
 * @since 1.0.0
 */
public class PageResult<T> {

    private final List<T> records;
    private final long total;
    private final int pageNum;
    private final int pageSize;
    private final int totalPages;

    /**
     * 构造分页响应。
     *
     * @param records  当前页记录
     * @param total    总记录数
     * @param pageNum  当前页码（1-based）
     * @param pageSize 每页大小
     */
    public PageResult(List<T> records, long total, int pageNum, int pageSize) {
        this.records = records == null ? List.of() : List.copyOf(records);
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    /**
     * 构造空分页响应。
     *
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param <T>      记录类型
     * @return 空的分页响应
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return new PageResult<>(List.of(), 0L, pageNum, pageSize);
    }

    /**
     * 由 Spring Data {@link Page} 构造分页响应：映射当前页内容，透传调用方 1-based 页码/页大小。
     *
     * <p>页码/页大小取调用方传入值（非 {@code page.getNumber()}），以保持既有 1-based 语义，
     * 不引入 0-based↔1-based 偏移（参见 PRD v1.3 §9.2 与既有控制器 adapter 约定）。</p>
     *
     * @param page     Spring Data 分页结果（提供 content 与 totalElements）
     * @param pageNum  当前页码（1-based，调用方语义）
     * @param pageSize 每页大小
     * @param mapper   实体 → 记录 DTO 映射函数
     * @param <E>      源实体类型
     * @param <T>      目标记录类型
     * @return 分页响应
     */
    public static <E, T> PageResult<T> from(
            final Page<E> page,
            final int pageNum,
            final int pageSize,
            final Function<? super E, ? extends T> mapper) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(mapper, "mapper");
        final List<T> records = page.getContent().stream()
                .<T>map(mapper)
                .toList();
        return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);
    }

    /**
     * 由已是目标记录类型的 Spring Data {@link Page} 构造分页响应（恒等映射，无需 mapper）。
     *
     * <p>适用于 service 已返回 {@code Page<T>}（{@code T} 即响应 DTO）的站点，
     * 避免调用方书写 {@code Function.identity()} 样板。语义等同
     * {@link #from(Page, int, int, Function)} 传入恒等函数。</p>
     *
     * @param page     Spring Data 分页结果（内容已是目标记录类型）
     * @param pageNum  当前页码（1-based，调用方语义）
     * @param pageSize 每页大小
     * @param <T>      记录类型
     * @return 分页响应
     */
    public static <T> PageResult<T> from(
            final Page<T> page,
            final int pageNum,
            final int pageSize) {
        return from(page, pageNum, pageSize, Function.identity());
    }

    public List<T> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public long getTotal() {
        return total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
