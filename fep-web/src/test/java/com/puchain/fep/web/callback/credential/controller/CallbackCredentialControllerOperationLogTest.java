package com.puchain.fep.web.callback.credential.controller;

import com.puchain.fep.web.callback.credential.dto.CallbackCredentialCreateRequest;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialUpdateRequest;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证回调凭证 Controller 5 方法均标注 {@link OperationLog}（PRD v1.3 §8.3 操作审计全覆盖）。
 *
 * <p>反射断言，不启动 Spring context，确定性高。AOP 写库行为由 {@code OperationLogAspect}
 * 既有测试覆盖，本测试仅确保注解存在且属性正确。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackCredentialControllerOperationLogTest {

    private OperationLog annotationOf(final String method, final Class<?>... params) throws NoSuchMethodException {
        final Method m = CallbackCredentialController.class.getMethod(method, params);
        final OperationLog log = m.getAnnotation(OperationLog.class);
        assertThat(log).as("method %s must be annotated with @OperationLog", method).isNotNull();
        assertThat(log.module()).isEqualTo("回调凭证管理");
        assertThat(log.description()).isNotBlank();
        return log;
    }

    @Test
    void create_hasCreateOperationLog() throws Exception {
        assertThat(annotationOf("create", CallbackCredentialCreateRequest.class).type())
                .isEqualTo(OperationType.CREATE);
    }

    @Test
    void get_hasQueryOperationLog() throws Exception {
        assertThat(annotationOf("get", String.class).type()).isEqualTo(OperationType.QUERY);
    }

    @Test
    void list_hasQueryOperationLog() throws Exception {
        assertThat(annotationOf("list").type()).isEqualTo(OperationType.QUERY);
    }

    @Test
    void update_hasUpdateOperationLog() throws Exception {
        assertThat(annotationOf("update", String.class, CallbackCredentialUpdateRequest.class).type())
                .isEqualTo(OperationType.UPDATE);
    }

    @Test
    void delete_hasDeleteOperationLog() throws Exception {
        assertThat(annotationOf("delete", String.class).type()).isEqualTo(OperationType.DELETE);
    }

    @Test
    void rotateKey_hasUpdateOperationLog() throws Exception {
        assertThat(annotationOf("rotateKey", String.class).type()).isEqualTo(OperationType.UPDATE);
    }
}
