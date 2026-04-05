package com.puchain.fep.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置。
 *
 * <p>提供 Swagger UI ({@code /swagger-ui.html}) 和 OpenAPI 规范
 * ({@code /v3/api-docs})，并定义 BearerAuth (JWT) 安全方案。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * 构建 OpenAPI 元数据及安全方案。
     *
     * @return 包含项目信息和 JWT BearerAuth 的 {@link OpenAPI} 实例
     */
    @Bean
    public OpenAPI fepOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FEP 综合前置平台 API")
                        .description("FEP (Front-End Processor) 综合前置平台管理 Web 后端 API")
                        .version("1.0.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("FEP Team")
                                .email("fep@puchain.com")))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 认证令牌")));
    }
}
