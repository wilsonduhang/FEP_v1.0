package com.puchain.fep.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 模块依赖方向校验 — 守护 FEP 8 模块的依赖边界（对应四层架构的模块化拆分）。
 *
 * <p>依赖方向（上层可依赖下层，反之不可）:</p>
 * <pre>
 * common (最底层)
 *   ← security-api
 *     ← security-mock (只被 web 依赖)
 *     ← transport
 *     ← converter
 *       ← processor (← transport, converter)
 *         ← collector
 *           ← web (依赖全部)
 * </pre>
 */
@AnalyzeClasses(
    packages = "com.puchain.fep",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ModuleDependencyTest {

    @ArchTest
    static final ArchRule layered_architecture_is_respected =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Common").definedBy("com.puchain.fep.common..")
            .layer("SecurityApi").definedBy("com.puchain.fep.security.api..")
            .layer("SecurityMock").definedBy("com.puchain.fep.security.mock..")
            .layer("Transport").definedBy("com.puchain.fep.transport..")
            .layer("Converter").definedBy("com.puchain.fep.converter..")
            .layer("Processor").definedBy("com.puchain.fep.processor..")
            .layer("Collector").definedBy("com.puchain.fep.collector..")
            .layer("Web").definedBy("com.puchain.fep.web..")

            // common 是底层，不依赖任何 fep 模块
            .whereLayer("Common").mayNotAccessAnyLayer()

            // security-api 只依赖 common
            .whereLayer("SecurityApi").mayOnlyAccessLayers("Common")
            .whereLayer("SecurityApi").mayOnlyBeAccessedByLayers(
                "SecurityMock", "Transport", "Converter",
                "Processor", "Collector", "Web"
            )

            // security-mock 只依赖 security-api (transitively common)，只被 web 使用
            .whereLayer("SecurityMock").mayOnlyAccessLayers("SecurityApi", "Common")
            .whereLayer("SecurityMock").mayOnlyBeAccessedByLayers("Web")

            // transport 依赖 common + security-api，被 converter（P1b TransportPayloadAdapter
            // 桥接）/ processor / web 使用
            .whereLayer("Transport").mayOnlyAccessLayers("Common", "SecurityApi")
            .whereLayer("Transport").mayOnlyBeAccessedByLayers("Converter", "Processor", "Web")

            // converter 依赖 common + security-api + transport（P1b TransportPayloadAdapter
            // 有意桥接 Converter.EncodeResult → Transport.TlqMessage），被 processor / web 使用
            .whereLayer("Converter").mayOnlyAccessLayers("Common", "SecurityApi", "Transport")
            .whereLayer("Converter").mayOnlyBeAccessedByLayers("Processor", "Web")

            // processor 依赖 common + security-api + transport + converter，被 collector / web 使用
            .whereLayer("Processor").mayOnlyAccessLayers("Common", "SecurityApi", "Transport", "Converter")
            .whereLayer("Processor").mayOnlyBeAccessedByLayers("Collector", "Web")

            // collector 依赖 common + security-api + processor，被 web 使用
            .whereLayer("Collector").mayOnlyAccessLayers("Common", "SecurityApi", "Processor")
            .whereLayer("Collector").mayOnlyBeAccessedByLayers("Web")

            // web 是顶层，不被任何模块依赖
            .whereLayer("Web").mayNotBeAccessedByAnyLayer();
}
