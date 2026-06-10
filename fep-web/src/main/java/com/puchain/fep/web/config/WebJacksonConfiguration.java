package com.puchain.fep.web.config;

import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.SpringHandlerInstantiator;

/**
 * 让 Jackson 经 Spring 容器实例化自定义序列化器/反序列化器（注入 Spring bean）。
 *
 * <p>{@link com.puchain.fep.web.common.desensitize.DesensitizeJsonSerializer} 依赖注入
 * {@code DesensitizeService} → 须经 {@link SpringHandlerInstantiator}（否则 Jackson 用无参
 * 反射实例化致注入失效、@Desensitize 静默不脱敏）。Spring Boot 自动将本 bean 应用到默认 ObjectMapper。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class WebJacksonConfiguration {

    /**
     * Spring 感知的 Jackson handler 实例化器。
     *
     * @param applicationContext 应用上下文
     * @return HandlerInstantiator
     */
    @Bean
    public HandlerInstantiator handlerInstantiator(final ApplicationContext applicationContext) {
        final AutowireCapableBeanFactory beanFactory =
                applicationContext.getAutowireCapableBeanFactory();
        return new SpringHandlerInstantiator(beanFactory);
    }
}
