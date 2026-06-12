package com.puchain.fep.web.sysmgmt.log.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * 审计行规范化确定性：手算 netstring 字面值锚定 + null/空串区分 + 纳秒截断稳定。
 */
class AuditCanonicalizerTest {

    private static SysOperationLog sample() {
        final SysOperationLog e = new SysOperationLog();
        e.setLogId("L1");
        e.setUserId(null);
        e.setUserAccount("u");
        e.setModule("模");
        e.setOperation(OperationType.QUERY);
        e.setDescription(null);
        e.setMethod("GET");
        e.setRequestUrl("/x");
        e.setRequestParams(null);
        e.setResponseStatus(200);
        e.setIpAddress("1.2.3.4");
        e.setDurationMs(5L);
        e.setCreateTime(LocalDateTime.of(2026, 6, 12, 10, 30, 5));
        e.setTraceId("t-1");
        return e;
    }

    @Test
    void canonicalize_matchesHandComputedNetstring() {
        // 逐字段手算：len = UTF-8 字节长（"模" = 3 字节）；null = -1:
        final String expected = "1:1"
                + "2:L1"
                + "-1:"
                + "1:u"
                + "3:模"
                + "5:QUERY"
                + "-1:"
                + "3:GET"
                + "2:/x"
                + "-1:"
                + "3:200"
                + "7:1.2.3.4"
                + "1:5"
                + "19:2026-06-12T10:30:05"
                + "3:t-1";
        assertThat(AuditCanonicalizer.canonicalize(sample(), 1L)).isEqualTo(expected);
    }

    @Test
    void canonicalize_isDeterministic() {
        final SysOperationLog e = sample();
        assertThat(AuditCanonicalizer.canonicalize(e, 7L))
                .isEqualTo(AuditCanonicalizer.canonicalize(e, 7L));
    }

    @Test
    void canonicalize_distinguishesNullFromEmptyString() {
        final SysOperationLog withNull = sample();
        final SysOperationLog withEmpty = sample();
        withEmpty.setUserAccount("");
        assertThat(AuditCanonicalizer.canonicalize(withNull, 1L))
                .isNotEqualTo(AuditCanonicalizer.canonicalize(withEmpty, 1L));
    }

    @Test
    void canonicalize_truncatesNanosToSeconds() {
        final SysOperationLog precise = sample();
        precise.setCreateTime(LocalDateTime.of(2026, 6, 12, 10, 30, 5, 123456789));
        // 纳秒精度输入与秒精度输入同输出（DB TIMESTAMP 往返稳定，抉择④）
        assertThat(AuditCanonicalizer.canonicalize(precise, 1L))
                .isEqualTo(AuditCanonicalizer.canonicalize(sample(), 1L));
    }
}
