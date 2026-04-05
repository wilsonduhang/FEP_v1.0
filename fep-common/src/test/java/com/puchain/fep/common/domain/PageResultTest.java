package com.puchain.fep.common.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PageResult 单元测试。
 */
class PageResultTest {

    @Test
    void constructorShouldComputeTotalPages() {
        PageResult<String> r = new PageResult<>(List.of("a", "b"), 21L, 1, 10);
        assertEquals(2, r.getRecords().size());
        assertEquals(21L, r.getTotal());
        assertEquals(1, r.getPageNum());
        assertEquals(10, r.getPageSize());
        assertEquals(3, r.getTotalPages());
    }

    @Test
    void emptyShouldReturnZeroRecords() {
        PageResult<String> r = PageResult.empty(1, 20);
        assertTrue(r.getRecords().isEmpty());
        assertEquals(0L, r.getTotal());
        assertEquals(0, r.getTotalPages());
    }

    @Test
    void nullRecordsShouldBecomeEmptyList() {
        PageResult<String> r = new PageResult<>(null, 0L, 1, 10);
        assertTrue(r.getRecords().isEmpty());
    }

    @Test
    void errorCodeDefaultMessageShouldBeRetrievable() {
        assertEquals("成功", FepErrorCode.SUCCESS.getDefaultMessage());
        assertEquals("PARAM_4001", FepErrorCode.PARAM_4001.getCode());
    }
}
