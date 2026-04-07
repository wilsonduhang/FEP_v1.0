package com.puchain.fep.common.exception;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.FepErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * GlobalExceptionHandler 单元测试。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionBiz5002ShouldReturn409() {
        FepBusinessException ex = new FepBusinessException(FepErrorCode.BIZ_5002, "用户已存在");
        ResponseEntity<ApiResult<Void>> response = handler.handleBusiness(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BIZ_5002", response.getBody().getCode());
        assertEquals("用户已存在", response.getBody().getMessage());
    }

    @Test
    void businessExceptionBiz5001ShouldReturn404() {
        FepBusinessException ex = new FepBusinessException(FepErrorCode.BIZ_5001, "资源不存在");
        ResponseEntity<ApiResult<Void>> response = handler.handleBusiness(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BIZ_5001", response.getBody().getCode());
    }

    @Test
    void businessExceptionOtherShouldReturn400() {
        FepBusinessException ex = new FepBusinessException(FepErrorCode.BIZ_5003, "业务状态不允许");
        ResponseEntity<ApiResult<Void>> response = handler.handleBusiness(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BIZ_5003", response.getBody().getCode());
    }

    @Test
    void authException401ShouldReturn401() {
        FepAuthException ex = new FepAuthException(FepErrorCode.AUTH_0401);
        ResponseEntity<ApiResult<Void>> response = handler.handleAuth(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTH_0401", response.getBody().getCode());
    }

    @Test
    void authException403ShouldReturn403() {
        FepAuthException ex = new FepAuthException(FepErrorCode.AUTH_0403);
        ResponseEntity<ApiResult<Void>> response = handler.handleAuth(ex);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void illegalArgumentShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("id must not be null");
        ResponseEntity<ApiResult<Void>> response = handler.handleIllegalArgument(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PARAM_4002", response.getBody().getCode());
        assertEquals("id must not be null", response.getBody().getMessage());
    }

    @Test
    void unexpectedExceptionShouldReturn500() {
        Exception ex = new RuntimeException("db connection lost");
        ResponseEntity<ApiResult<Void>> response = handler.handleUnexpected(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SYS_0500", response.getBody().getCode());
        // 不暴露内部细节
        assertEquals("系统繁忙，请稍后再试", response.getBody().getMessage());
    }
}
