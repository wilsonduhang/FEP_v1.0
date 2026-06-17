package com.puchain.fep.web.audit.review.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 审核工作流模块配置：注册 {@link ReviewWorkflowProperties}
 * （与项目既有 {@code @EnableConfigurationProperties} 范式一致，
 * 如 {@code CallbackAlertConfiguration}）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(ReviewWorkflowProperties.class)
public class ReviewWorkflowConfiguration {
}
