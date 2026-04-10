package com.puchain.fep.converter;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * fep-converter 模块自动配置类。
 *
 * <p>通过 {@code @ComponentScan} 扫描 converter 包下所有 {@code @Component}/{@code @Service}，
 * 使 XmlCodec、MessageSigner、MessageEncoder 等 bean 可被 Spring 上下文管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = "com.puchain.fep.converter")
public class ConverterAutoConfiguration {
}
