package com.puchain.fep.transport.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * fep-transport 架构边界守护测试。
 *
 * <p>P1c Plan v1d Task 8 实现：fep-transport 首个 ArchUnit 测试，覆盖 7 条规则，
 * 防止 API/SDK/Mock 层之间的反向依赖以及跨模块引入业务层。
 *
 * <p>规则一览：
 * <ul>
 *   <li>R1 — {@code transport.api.*} 不依赖 {@code com.tongtech..} SDK 类型，
 *       保证 API 接口与具体 SDK 实现解耦。</li>
 *   <li>R2 — {@code transport.api.*} 不依赖 {@code transport.tongtech.*} 或
 *       {@code transport.mock.*}，API 仅定义契约。</li>
 *   <li>R3 — {@code transport.tongtech.*} 不依赖 {@code transport.mock.*}，
 *       两个 Provider 实现互斥。</li>
 *   <li>R4 — {@code transport.mock.*} 不依赖 {@code transport.tongtech.*} 或
 *       {@code com.tongtech..}，Mock 必须独立可在无 SDK 环境下编译运行。</li>
 *   <li>R5 — {@code transport.tongtech.error.*} 不依赖 adapter 或 lifecycle，
 *       保持错误码映射器的独立可测试性。</li>
 *   <li>R6 (v1b 降级) — {@code transport.tongtech.config.*} 不依赖
 *       {@code transport.tongtech.lifecycle..}，config 仅装配 Bean，不携业务状态机
 *       逻辑；adapter 因 {@code @Bean} 工厂参数注入合法，无法在 ArchUnit 1.3
 *       表达力内精确守护，归 IT-bridge follow-up（关注 ArchUnit 7.x 升级）。</li>
 *   <li>R7 — {@code com.puchain.fep.transport..} 不依赖
 *       {@code com.puchain.fep.converter..} 或 {@code com.puchain.fep.web..}，
 *       fep-transport 是单向被依赖模块（v1a B-P1-3 跨模块解耦守护）。</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TransportArchitectureTest {

    private static JavaClasses transportClasses;

    @BeforeAll
    static void importClasses() {
        transportClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.puchain.fep.transport");
    }

    /**
     * R1：API 层不感知 TongLINK/Q SDK 类型。
     */
    @Test
    void apiLayerMustNotDependOnTongtechSdk() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.transport.api..")
                .should().dependOnClassesThat().resideInAPackage("com.tongtech..")
                .because("transport.api 仅定义接口契约，不应感知具体 SDK 类型");
        rule.check(transportClasses);
    }

    /**
     * R2：API 层不依赖任何 Provider 实现包。
     */
    @Test
    void apiLayerMustNotDependOnImplementations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.transport.api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.puchain.fep.transport.tongtech..",
                        "com.puchain.fep.transport.mock..")
                .because("transport.api 仅定义契约，不应依赖 tongtech / mock 实现包（反向依赖方向）");
        rule.check(transportClasses);
    }

    /**
     * R3：TongLINK/Q Provider 与 Mock Provider 互斥。
     */
    @Test
    void tongtechMustNotDependOnMock() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.transport.tongtech..")
                .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.transport.mock..")
                .because("tongtech 与 mock 是两套互斥 Provider，禁止交叉依赖");
        rule.check(transportClasses);
    }

    /**
     * R4：Mock 实现独立可运行，禁止引入 SDK 或 tongtech 实现。
     */
    @Test
    void mockMustNotDependOnTongtechOrSdk() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.transport.mock..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.puchain.fep.transport.tongtech..",
                        "com.tongtech..")
                .because("mock 是开发占位实现，必须独立可在无 SDK 环境下编译运行");
        rule.check(transportClasses);
    }

    /**
     * R5：错误码映射器是基础工具，独立可测，不依赖业务实现。
     */
    @Test
    void errorMapperMustNotDependOnAdapterOrLifecycle() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.transport.tongtech.error..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.puchain.fep.transport.tongtech.adapter..",
                        "com.puchain.fep.transport.tongtech.lifecycle..")
                .because("tongtech.error 是基础映射工具，不应依赖 adapter / lifecycle 业务实现");
        rule.check(transportClasses);
    }

    /**
     * R6（v1b 降级）：config 包不携业务状态机逻辑。
     *
     * <p>Plan v1d 要求严格守护「config 不调用 adapter/lifecycle 实例方法」，但 ArchUnit 1.3
     * 在方法级 {@code callCodeUnitWhere} 表达上对 {@code @Bean} 工厂方法的合法注入参数
     * 难以精确排除，故按 plan 行 2598 提供的 fallback 降级为「config 不依赖 lifecycle」。
     *
     * <p>为何不守护 adapter：{@code TongtechProducerConfiguration} 等 {@code @Bean} 工厂方法
     * 必须以 adapter 类作为返回类型/构造参数才能完成 Spring 装配，是合法依赖。该缺口归
     * IT-bridge follow-up，待 ArchUnit 7.x 升级后再细化。
     */
    @Test
    void configMustNotDependOnLifecycle() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.transport.tongtech.config..")
                .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.transport.tongtech.lifecycle..")
                .because("config 包仅装配 Bean，不应引用 lifecycle 业务状态机");
        rule.check(transportClasses);
    }

    /**
     * R7：fep-transport 是单向被依赖模块，不引用 fep-converter / fep-web。
     */
    @Test
    void transportMustNotDependOnConverterOrWeb() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.transport..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.puchain.fep.converter..",
                        "com.puchain.fep.web..")
                .because("fep-transport 单向被依赖；9006/9008 拼装位于 fep-web 跨模块解耦守护（v1a B-P1-3）");
        rule.check(transportClasses);
    }
}
