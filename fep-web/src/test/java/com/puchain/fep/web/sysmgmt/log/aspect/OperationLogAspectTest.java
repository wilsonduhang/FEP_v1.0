package com.puchain.fep.web.sysmgmt.log.aspect;

import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.audit.AuditChainWriter;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link OperationLogAspect} 单元测试。
 *
 * <p><b>注意</b>：AOP 集成测试依赖 Task 3 中带有 {@link OperationLog} 注解的 Controller 方法，
 * 因此 Spring Boot 集成场景暂不在本 Task 中验证。本文件当前仅校验核心类可正常编译，
 * Task 3 完成后补充完整的 {@code @SpringBootTest} 集成测试。</p>
 *
 * <p>编译验证覆盖：</p>
 * <ul>
 *   <li>{@link OperationLogAspect} 可被实例化（构造方法签名正确）</li>
 *   <li>{@link SysOperationLog} 字段赋值和 getter 行为正确</li>
 *   <li>{@link OperationType} 枚举常量可正常引用</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OperationLogAspectTest {

    /**
     * 验证 {@link SysOperationLog} 的 setter/getter 基本正确性。
     */
    @Test
    void sysOperationLog_settersAndGetters_shouldWork() {
        SysOperationLog entity = new SysOperationLog();
        entity.setLogId("abc123");
        entity.setModule("系统管理-用户");
        entity.setOperation(OperationType.CREATE);
        entity.setResponseStatus(200);
        entity.setIpAddress("127.0.0.1");
        entity.setDurationMs(50L);
        entity.setMethod("POST");
        entity.setRequestUrl("/api/v1/sys/users");

        assertEquals("abc123", entity.getLogId());
        assertEquals("系统管理-用户", entity.getModule());
        assertEquals(OperationType.CREATE, entity.getOperation());
        assertEquals(200, entity.getResponseStatus());
        assertEquals("127.0.0.1", entity.getIpAddress());
        assertEquals(50L, entity.getDurationMs());
        assertEquals("POST", entity.getMethod());
        assertEquals("/api/v1/sys/users", entity.getRequestUrl());
    }

    /**
     * 验证 {@link OperationType} 包含所有预期枚举常量。
     */
    @Test
    void operationType_allConstantsExist() {
        assertNotNull(OperationType.QUERY);
        assertNotNull(OperationType.CREATE);
        assertNotNull(OperationType.UPDATE);
        assertNotNull(OperationType.DELETE);
        assertNotNull(OperationType.LOGIN);
        assertNotNull(OperationType.LOGOUT);
        assertNotNull(OperationType.EXPORT);
        assertNotNull(OperationType.OTHER);
        assertEquals(8, OperationType.values().length);
    }

    /**
     * 验证 {@link OperationLogAspect} 可使用 mock {@link AuditChainWriter} 构造
     * （GM S5：落库依赖由 Repository 换为链式写入器）。
     *
     * <p>完整链路（切点触发 → append 链式落库）由 {@code AuditChainWriterTest} +
     * T6 mock 全 context intact 用例覆盖。</p>
     */
    @Test
    void operationLogAspect_canBeConstructed() {
        OperationLogAspect aspect = new OperationLogAspect(Mockito.mock(AuditChainWriter.class));
        assertNotNull(aspect);
    }

    /**
     * 行为断言（Plan B-2）：around 经 AuditChainWriter.append 落库（非直写 Repository）。
     */
    @Test
    void around_delegatesPersistenceToChainWriter() throws Throwable {
        AuditChainWriter writer = Mockito.mock(AuditChainWriter.class);
        OperationLogAspect aspect = new OperationLogAspect(writer);
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/sys/logs");
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
        try {
            org.aspectj.lang.ProceedingJoinPoint joinPoint =
                    Mockito.mock(org.aspectj.lang.ProceedingJoinPoint.class);
            Mockito.when(joinPoint.proceed()).thenReturn("ok");
            OperationLog annotation = Mockito.mock(OperationLog.class);
            Mockito.when(annotation.module()).thenReturn("日志管理");
            Mockito.when(annotation.type()).thenReturn(OperationType.QUERY);
            Mockito.when(annotation.description()).thenReturn("");
            assertEquals("ok", aspect.around(joinPoint, annotation));
            Mockito.verify(writer).append(Mockito.any(SysOperationLog.class));
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
    }
}
