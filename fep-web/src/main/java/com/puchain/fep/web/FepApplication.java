package com.puchain.fep.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * FEP 综合前置平台 — Spring Boot 主应用。
 *
 * <p>扫描 {@code com.puchain.fep} 下所有模块的组件。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.puchain.fep")
public class FepApplication {

    /**
     * 应用程序入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FepApplication.class, args);
    }
}
