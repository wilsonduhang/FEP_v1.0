package com.puchain.fep.common.domain;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void fromShouldMapContentAndPreserveRequestPaging() {
        Page<String> page = new PageImpl<>(
                List.of("a", "b"),
                PageRequest.of(0, 10),
                23L);
        PageResult<Integer> r = PageResult.from(page, 1, 10, String::length);
        assertEquals(List.of(1, 1), r.getRecords());
        assertEquals(23L, r.getTotal());
        assertEquals(1, r.getPageNum());
        assertEquals(10, r.getPageSize());
        assertEquals(3, r.getTotalPages());
    }

    @Test
    void fromEmptyPageShouldReturnEmptyRecords() {
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(2, 10), 0L);
        PageResult<Integer> r = PageResult.from(page, 3, 10, String::length);
        assertTrue(r.getRecords().isEmpty());
        assertEquals(0L, r.getTotal());
        assertEquals(3, r.getPageNum());
        assertEquals(0, r.getTotalPages());
    }

    @Test
    void fromNullMapperShouldThrowNpe() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 1L);
        assertThrows(NullPointerException.class, () -> PageResult.from(page, 1, 10, null));
    }

    @Test
    void errorCodeDefaultMessageShouldBeRetrievable() {
        assertEquals("成功", FepErrorCode.SUCCESS.getDefaultMessage());
        assertEquals("PARAM_4001", FepErrorCode.PARAM_4001.getCode());
    }
}
