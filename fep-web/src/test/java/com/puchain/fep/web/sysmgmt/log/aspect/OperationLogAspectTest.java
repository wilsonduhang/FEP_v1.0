package com.puchain.fep.web.sysmgmt.log.aspect;

import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.audit.AuditChainWriter;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link OperationLogAspect} 单元测试：entity/枚举基本语义 + around 落库委托行为
 * （池③ 类头 doc-rot 刷新——原"Task 3 后补集成测试"注记已过时）。
 *
 * <p>AOP 集成链路（切点触发 → {@link AuditChainWriter#append} 链式落库）由
 * {@code AuditChainWriterTest} + {@code SysOperationLogControllerTest} 的
 * {@code /integrity} 端到端用例覆盖（GM S5 T6），本类聚焦切面自身行为。</p>
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
     * 行为断言（GM S5 B-2；池③ 折叠原 canBeConstructed 弱断言用例——构造正确性
     * 由本用例的真实构造隐含覆盖）：around 经 AuditChainWriter.append 落库（非直写 Repository）。
     */
    @Test
    void around_delegatesPersistenceToChainWriter() throws Throwable {
        AuditChainWriter writer = Mockito.mock(AuditChainWriter.class);
        OperationLogAspect aspect = new OperationLogAspect(writer);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/sys/logs");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
            Mockito.when(joinPoint.proceed()).thenReturn("ok");
            OperationLog annotation = Mockito.mock(OperationLog.class);
            Mockito.when(annotation.module()).thenReturn("日志管理");
            Mockito.when(annotation.type()).thenReturn(OperationType.QUERY);
            Mockito.when(annotation.description()).thenReturn("");
            assertEquals("ok", aspect.around(joinPoint, annotation));
            Mockito.verify(writer).append(Mockito.any(SysOperationLog.class));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
