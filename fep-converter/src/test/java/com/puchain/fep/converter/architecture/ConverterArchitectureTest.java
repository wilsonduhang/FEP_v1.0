package com.puchain.fep.converter.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * fep-converter 模块的架构守护规则。
 *
 * <p>强制以下边界：</p>
 * <ol>
 *   <li>converter 模块不得依赖 BouncyCastle（禁止直接实现国密算法，
 *       必须通过 {@code security-api} 委派给 security-impl）</li>
 *   <li>converter 模块不得依赖 {@code security.impl} 或 {@code security.mock}
 *       实现包（只能面向 {@code security.api} 接口编程）</li>
 *   <li>{@code converter.model.*} 不得反向依赖 {@code converter.pipeline.*}
 *       （model 是底层，pipeline 是高层，避免循环依赖）</li>
 * </ol>
 *
 * <p>使用 {@link ImportOption.Predefined#DO_NOT_INCLUDE_TESTS} 排除测试类，
 * 因为测试类（如 {@code MessagePipelineIT}）合法地依赖 security.mock。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class ConverterArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.puchain.fep.converter");

    @Test
    void converterMustNotDependOnBouncyCastle() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("org.bouncycastle..")
                .because("converter 必须通过 security-api 调用密码原语，禁止直连 BouncyCastle")
                .check(CLASSES);
    }

    @Test
    void converterMustNotDependOnSecurityImplOrMock() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.puchain.fep.security.impl..",
                        "com.puchain.fep.security.mock..")
                .because("converter 仅允许依赖 security-api 接口，impl/mock 由 Spring 运行时注入")
                .check(CLASSES);
    }

    @Test
    void modelMustNotDependOnPipeline() {
        noClasses()
                .that().resideInAPackage("..converter.model..")
                .should().dependOnClassesThat().resideInAPackage("..converter.pipeline..")
                .because("model 是底层领域对象，pipeline 是高层编排，禁止反向依赖")
                .check(CLASSES);
    }
}
