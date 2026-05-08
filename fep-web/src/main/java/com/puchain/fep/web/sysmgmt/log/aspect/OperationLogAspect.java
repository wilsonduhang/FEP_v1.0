package com.puchain.fep.web.sysmgmt.log.aspect;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.TextUtil;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面，拦截标注了 {@link OperationLog} 注解的方法，自动记录审计日志。
 *
 * <p>日志记录失败不影响业务请求，所有异常在内部捕获后仅打印 WARN 日志。
 * 目标方法抛出异常时响应状态记录为 500。</p>
 *
 * <p>参见 PRD v1.3 §8.3 操作审计日志全覆盖。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    /** 请求参数最大截断长度。 */
    private static final int PARAMS_MAX_LENGTH = 2000;

    /** 正常响应状态码。 */
    private static final int HTTP_OK = 200;

    /** 服务器内部错误状态码。 */
    private static final int HTTP_INTERNAL_ERROR = 500;

    private final SysOperationLogRepository operationLogRepository;

    /**
     * 构造 OperationLogAspect。
     *
     * @param operationLogRepository 操作日志 Repository
     */
    public OperationLogAspect(final SysOperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    /**
     * 环绕通知：拦截 {@link OperationLog} 注解方法，记录操作日志。
     *
     * @param joinPoint  切点
     * @param annotation 方法上的 {@link OperationLog} 注解实例
     * @return 目标方法返回值
     * @throws Throwable 目标方法抛出的异常（直接向上传播）
     */
    @Around("@annotation(annotation)")
    public Object around(final ProceedingJoinPoint joinPoint, final OperationLog annotation) throws Throwable {
        long startMillis = System.currentTimeMillis();
        int responseStatus = HTTP_OK;

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            responseStatus = HTTP_INTERNAL_ERROR;
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - startMillis;
            saveLog(annotation, responseStatus, durationMs);
        }
    }

    /**
     * 构建并持久化操作日志。
     *
     * <p>日志保存失败时仅输出 WARN，不向上抛出异常，确保日志故障不影响主业务。</p>
     *
     * @param annotation     操作日志注解
     * @param responseStatus HTTP 响应状态码
     * @param durationMs     请求耗时（毫秒）
     */
    private void saveLog(final OperationLog annotation, final int responseStatus, final long durationMs) {
        try {
            HttpServletRequest request = resolveRequest();
            if (request == null) {
                log.warn("OperationLogAspect: HttpServletRequest not available, skipping log");
                return;
            }

            SysOperationLog entity = new SysOperationLog();
            entity.setLogId(IdGenerator.uuid32());
            entity.setModule(annotation.module());
            entity.setOperation(annotation.type());
            entity.setDescription(annotation.description().isEmpty() ? null : annotation.description());
            entity.setMethod(request.getMethod());
            entity.setRequestUrl(request.getRequestURI());
            entity.setRequestParams(TextUtil.truncate(request.getQueryString(), PARAMS_MAX_LENGTH));
            entity.setResponseStatus(responseStatus);
            entity.setIpAddress(resolveClientIp(request));
            entity.setDurationMs(durationMs);
            entity.setCreateTime(LocalDateTime.now());

            fillCurrentUser(entity);

            operationLogRepository.save(entity);
        } catch (Exception ex) {
            log.warn("OperationLogAspect: failed to save operation log, cause: {}", ex.getMessage());
        }
    }

    /**
     * 从 SecurityContext 中提取当前用户信息并填充到日志实体。
     *
     * <p>认证主体名称约定为 userId（由 {@code JwtAuthFilter} 写入）。
     * 账号字段暂留空，后续可通过用户服务按 ID 查询补充。</p>
     *
     * @param entity 操作日志实体
     */
    private void fillCurrentUser(final SysOperationLog entity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String principal) {
            entity.setUserId(principal);
            // userAccount 由 Task 3 的 Service 层按需补充
        }
    }

    /**
     * 从 RequestContextHolder 获取当前 HTTP 请求。
     *
     * @return {@link HttpServletRequest}，若不在 Servlet 上下文则返回 null
     */
    private HttpServletRequest resolveRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /**
     * 解析客户端真实 IP，依次取 X-Forwarded-For、X-Real-IP、remoteAddr。
     *
     * @param request HTTP 请求
     * @return 客户端 IP 字符串
     */
    private String resolveClientIp(final HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个 IP，取第一个
            int commaIndex = ip.indexOf(',');
            return commaIndex > 0 ? ip.substring(0, commaIndex).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

}
