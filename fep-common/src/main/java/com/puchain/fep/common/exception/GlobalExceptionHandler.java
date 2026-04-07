package com.puchain.fep.common.exception;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.util.LogSanitizer;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * FEP 全局异常处理器。
 *
 * <p>将所有未捕获异常统一转换为 {@link ApiResult} 格式，保证响应结构一致。</p>
 *
 * <p>参见 PRD v1.3 &sect;9.1。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * <p>BIZ_5002（资源已存在）映射为 HTTP 409 Conflict；
     * BIZ_5001（资源不存在）映射为 HTTP 404 Not Found；
     * 其余业务异常映射为 HTTP 400 Bad Request。</p>
     *
     * @param ex 业务异常
     * @return HTTP 响应
     */
    @ExceptionHandler(FepBusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(FepBusinessException ex) {
        LOG.warn("Business exception: code={}, message={}",
                LogSanitizer.sanitize(ex.getErrorCode().getCode()), LogSanitizer.sanitize(ex.getMessage()));
        HttpStatus status;
        if (ex.getErrorCode() == FepErrorCode.BIZ_5002) {
            status = HttpStatus.CONFLICT;
        } else if (ex.getErrorCode() == FepErrorCode.BIZ_5001) {
            status = HttpStatus.NOT_FOUND;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status)
                .body(ApiResult.failure(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 处理认证/授权异常。
     *
     * @param ex 认证异常
     * @return HTTP 401 或 403 响应
     */
    @ExceptionHandler(FepAuthException.class)
    public ResponseEntity<ApiResult<Void>> handleAuth(FepAuthException ex) {
        LOG.warn("Auth exception: code={}, message={}",
                LogSanitizer.sanitize(ex.getErrorCode().getCode()), LogSanitizer.sanitize(ex.getMessage()));
        HttpStatus status = ex.getErrorCode() == FepErrorCode.AUTH_0403
                ? HttpStatus.FORBIDDEN
                : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status)
                .body(ApiResult.failure(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 处理 Bean Validation 校验失败。
     *
     * @param ex 校验异常
     * @return HTTP 400 响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        LOG.warn("Validation failed: {}", LogSanitizer.sanitize(message));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.failure(FepErrorCode.PARAM_4002, message));
    }

    /**
     * 处理约束违反异常。
     *
     * @param ex 约束违反异常
     * @return HTTP 400 响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        LOG.warn("Constraint violation: {}", LogSanitizer.sanitize(message));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.failure(FepErrorCode.PARAM_4002, message));
    }

    /**
     * 处理非法参数异常。
     *
     * @param ex 非法参数异常
     * @return HTTP 400 响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        LOG.warn("Illegal argument: {}", LogSanitizer.sanitize(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.failure(FepErrorCode.PARAM_4002, ex.getMessage()));
    }

    /**
     * 兜底处理所有未预期异常（不暴露内部细节）。
     *
     * @param ex 未预期异常
     * @return HTTP 500 响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception ex) {
        LOG.error("Unexpected exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.failure(FepErrorCode.SYS_0500, "系统繁忙，请稍后再试"));
    }

}
