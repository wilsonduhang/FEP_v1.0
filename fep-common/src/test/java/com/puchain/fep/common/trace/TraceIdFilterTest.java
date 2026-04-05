package com.puchain.fep.common.trace;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TraceIdFilter 单元测试。
 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void shouldGenerateTraceIdWhenNoneInHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        String[] captured = new String[1];
        FilterChain chain = (r, rs) -> captured[0] = MDC.get("traceId");

        filter.doFilter(req, res, chain);

        assertNotNull(captured[0]);
        assertTrue(captured[0].matches("\\d{14}-\\d{6}"));
        assertEquals(captured[0], res.getHeader("X-Trace-Id"));
    }

    @Test
    void shouldReuseTraceIdFromHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Trace-Id", "upstream-trace-123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        String[] captured = new String[1];
        FilterChain chain = (r, rs) -> captured[0] = MDC.get("traceId");

        filter.doFilter(req, res, chain);

        assertEquals("upstream-trace-123", captured[0]);
        assertEquals("upstream-trace-123", res.getHeader("X-Trace-Id"));
    }

    @Test
    void shouldClearMdcAfterRequest() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, (r, rs) -> { /* no-op */ });
        assertNull(MDC.get("traceId"));
    }
}
