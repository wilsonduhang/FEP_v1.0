package com.puchain.fep.common.domain;

import java.util.Collections;
import java.util.List;

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
