package com.puchain.fep.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TraceId 生成过滤器。
 *
 * <p>为每个请求生成唯一 traceId，写入 SLF4J MDC 和响应 HTTP header。</p>
 *
 * <p>格式: {yyyyMMddHHmmss}-{6 位递增序号}，共 21 字符。</p>
 *
 * <p>如果请求 header 已携带 {@code X-Trace-Id}，则复用（支持链路追踪）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** HTTP header 名称。 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC key 名称。 */
    public static final String MDC_KEY = "traceId";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final long SEQ_MODULO = 1_000_000L;
    private static final AtomicLong COUNTER = new AtomicLong(0);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String generateTraceId() {
        String time = LocalDateTime.now().format(TIME_FMT);
        long seq = COUNTER.incrementAndGet() % SEQ_MODULO;
        return time + "-" + String.format("%06d", seq);
    }
}
