package com.puchain.fep.common.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiResult 单元测试。
 */
class ApiResultTest {

    @Test
    void successWithDataShouldReturnCode200() {
        ApiResult<String> result = ApiResult.success("hello");
        assertEquals("200", result.getCode());
        assertEquals("成功", result.getMessage());
        assertEquals("hello", result.getData());
        assertNotNull(result.getTraceId());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void successWithoutDataShouldReturnNullData() {
        ApiResult<Void> result = ApiResult.success();
        assertEquals("200", result.getCode());
        assertNull(result.getData());
    }

    @Test
    void failureShouldCarryErrorCodeAndMessage() {
        ApiResult<Void> result = ApiResult.failure(FepErrorCode.PARAM_4001, "用户名不能为空");
        assertEquals("PARAM_4001", result.getCode());
        assertEquals("用户名不能为空", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void failureWithDefaultMessageShouldUseEnumMessage() {
        ApiResult<Void> result = ApiResult.failure(FepErrorCode.AUTH_0401);
        assertEquals("AUTH_0401", result.getCode());
        assertEquals(FepErrorCode.AUTH_0401.getDefaultMessage(), result.getMessage());
    }

    @Test
    void traceIdShouldBeReadFromMdcIfPresent() {
        org.slf4j.MDC.put("traceId", "trace-abc-123");
        try {
            ApiResult<String> result = ApiResult.success("x");
            assertEquals("trace-abc-123", result.getTraceId());
        } finally {
            org.slf4j.MDC.clear();
        }
    }
}
