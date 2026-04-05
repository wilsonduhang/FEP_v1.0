package com.puchain.fep.common.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PageQuery 单元测试。
 */
class PageQueryTest {

    @Test
    void offsetShouldReturnZeroForFirstPage() {
        PageQuery q = new PageQuery();
        q.setPageNum(1);
        q.setPageSize(20);
        assertEquals(0, q.offset());
    }

    @Test
    void offsetShouldReturnPageSizeForSecondPage() {
        PageQuery q = new PageQuery();
        q.setPageNum(2);
        q.setPageSize(20);
        assertEquals(20, q.offset());
    }

    @Test
    void defaultValuesShouldBePageNum1AndPageSize20() {
        PageQuery q = new PageQuery();
        assertEquals(1, q.getPageNum());
        assertEquals(20, q.getPageSize());
        assertEquals("DESC", q.getSortDir());
    }
}
