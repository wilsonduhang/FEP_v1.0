package com.puchain.fep.common.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询入参基类。
 *
 * <p>参见 PRD v1.3 §9.2 列表与分页规范。</p>
 *
 * <p>约定: 1-based pageNum, 默认 pageSize=20, 最大 pageSize=100。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class PageQuery {

    /** 默认每页大小 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "pageNum 必须 >= 1")
    private int pageNum = 1;

    /** 每页大小，默认 20，最大 100 */
    @Min(value = 1, message = "pageSize 必须 >= 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private int pageSize = DEFAULT_PAGE_SIZE;

    /** 排序字段（可选） */
    private String sortBy;

    /** 排序方向: ASC/DESC（可选，默认 DESC） */
    private String sortDir = "DESC";

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDir() {
        return sortDir;
    }

    public void setSortDir(String sortDir) {
        this.sortDir = sortDir;
    }

    /** 计算 Spring Data 的 offset（0-based）。 */
    public int offset() {
        return (pageNum - 1) * pageSize;
    }
}
