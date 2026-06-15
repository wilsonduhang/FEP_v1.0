package com.puchain.fep.web.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Dashboard 实时告警 WebSocket 注册配置。
 *
 * <p>把 {@link DashboardWebSocketHandler} 注册到端点 {@value #DASHBOARD_WS_PATH}
 * （muzhou 2026-06-15 签字：原生 {@code TextWebSocketHandler}，非 STOMP）。
 * 握手 origin 复用 {@code fep.cors.allowed-origins} 白名单（与
 * {@code SecurityConfiguration} CORS 同源，禁通配硬编码）；鉴权由 handler 首帧自管，
 * 故该端点已在 {@code SecurityConfiguration.PUBLIC_PATHS} 放行 Spring Security。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSocket
public class DashboardWebSocketConfiguration implements WebSocketConfigurer {

    /** Dashboard 实时推送 WebSocket 端点路径。 */
    public static final String DASHBOARD_WS_PATH = "/ws/dashboard";

    private final DashboardWebSocketHandler handler;
    private final String allowedOrigins;

    /**
     * 构造配置。
     *
     * @param handler        WebSocket 处理器
     * @param allowedOrigins 允许的握手 origin 白名单（逗号分隔，复用 CORS 配置）
     */
    public DashboardWebSocketConfiguration(final DashboardWebSocketHandler handler,
            @Value("${fep.cors.allowed-origins:http://localhost:*}") final String allowedOrigins) {
        this.handler = Objects.requireNonNull(handler, "handler");
        this.allowedOrigins = Objects.requireNonNull(allowedOrigins, "allowedOrigins");
    }

    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        final List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        registry.addHandler(handler, DASHBOARD_WS_PATH)
                .setAllowedOriginPatterns(origins.toArray(String[]::new));
    }
}
