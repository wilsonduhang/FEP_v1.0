package com.puchain.fep.web.outbound.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Placeholder {@link OutboundQueueRunner} 实现 (P5 T2).
 *
 * <p>使用 {@code @Configuration + @Bean + @ConditionalOnMissingBean} 这一 Spring Boot
 * 推荐方式（详见
 * <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.condition-annotations.bean-conditions">
 * Bean Conditions 文档</a>）。当 application context 中已存在任何其他
 * {@link OutboundQueueRunner} bean 时，本 stub 自动让位。</p>
 *
 * <p>作用：</p>
 * <ul>
 *   <li>保证 T2 阶段 {@link OutboundQueueConsumer} 在 production 启动时不会因
 *       missing dependency 抛 {@code NoSuchBeanDefinitionException}</li>
 *   <li>poll 命中时仅打 WARN 日志，让运维通过日志看到"还没装上真实 Runner"</li>
 * </ul>
 *
 * <p><b>T9 替换为 OutboundQueueRunnerImpl 后由 implementer 删除/降级到 src/test</b>
 * （Plan §Task 2 v0.6 N-NEW-2 显式 follow-up）。删除前请确认：</p>
 * <ol>
 *   <li>{@code OutboundQueueRunnerImpl} 已用 {@code @Component / @Service} 注册到容器</li>
 *   <li>对应 Plan §Task 9 完成并合并</li>
 *   <li><b>{@code @EnableConfigurationProperties(OutboundQueueProperties.class)}
 *       必须迁移到新配置类</b>（如 {@code OutboundConsumerConfiguration} 或 T9 里的
 *       runner impl 同侧 config），否则 {@link OutboundQueueProperties} bean 缺失</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(OutboundQueueProperties.class)
public class OutboundQueueRunnerStubConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundQueueRunnerStubConfiguration.class);

    /**
     * 注册一个回退 Runner bean（仅在没有其他 {@link OutboundQueueRunner} bean 时生效）。
     *
     * @return 仅打 WARN 日志的占位实现
     */
    @Bean
    @ConditionalOnMissingBean(OutboundQueueRunner.class)
    public OutboundQueueRunner stubOutboundQueueRunner() {
        return queueId -> LOG.warn(
                "OutboundQueueRunnerStubConfiguration.run({}) - T9 OutboundQueueRunnerImpl pending",
                queueId);
    }
}
