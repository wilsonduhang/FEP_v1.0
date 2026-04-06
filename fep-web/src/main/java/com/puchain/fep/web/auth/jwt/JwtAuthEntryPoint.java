package com.puchain.fep.web.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.FepErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证请求返回统一 401 JSON。
 *
 * <p>当请求未携带有效 JWT 时，Spring Security 调用此入口点，
 * 返回 {@link ApiResult} 格式的 401 响应。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * 构造 JwtAuthEntryPoint。
     *
     * @param objectMapper Jackson 序列化器
     */
    public JwtAuthEntryPoint(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResult.failure(FepErrorCode.AUTH_0401));
    }
}
