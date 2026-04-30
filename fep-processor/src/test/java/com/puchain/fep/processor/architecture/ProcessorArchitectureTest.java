package com.puchain.fep.processor.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ProcessorArchitectureTest {

    private static JavaClasses processorClasses;

    @BeforeAll
    static void importClasses() {
        processorClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.puchain.fep.processor");
    }

    @Test
    void processorMustNotDependOnSecurityImpl() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.processor..")
                .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.security.impl..")
                .because("security.impl 必须由安全专家人工编写，业务处理层仅可依赖 security.api");
        rule.check(processorClasses);
    }

    @Test
    void processorMustNotDependOnWebLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.processor..")
                .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.web..")
                .because("业务处理层应被 Web 层调用，反向依赖会破坏分层");
        rule.check(processorClasses);
    }

    @Test
    void bodyClassesMustExtendCfxBody() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.puchain.fep.processor.body..")
                .and().areNotInterfaces()
                .and().areTopLevelClasses()
                .and().haveSimpleNameNotContaining("package-info")
                .should().beAssignableTo(com.puchain.fep.converter.model.CfxBody.class)
                .because("所有报文 Body 必须继承 CfxBody 以便 fep-converter 流水线识别")
                .allowEmptyShould(true);
        rule.check(processorClasses);
    }

    @Test
    void routingShouldNotDependOnBodyOrValidation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.processor.routing..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.puchain.fep.processor.body..",
                        "com.puchain.fep.processor.validation..")
                .because("routing 是独立协议层，不应依赖 body 或 validation（反向依赖方向）");
        rule.check(processorClasses);
    }

    /**
     * R5（P4 T0 引入）：intake.port 必须保持极简，仅承载跨模块 collector→fep-web 契约的
     * Port + DTO，不得依赖 processor 内任何业务实现包（pipeline / state / reconciliation）。
     *
     * <p>该规则在 intake.port 子包尚未引入时（T7a 前）以 trivial PASS 形式存在，作为
     * 前置守护防止 T7a 实施时悄悄拉入 processor 业务依赖。</p>
     */
    @Test
    void intakePortMustStayMinimal() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.processor.intake.port..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.puchain.fep.processor.pipeline..",
                        "com.puchain.fep.processor.state..",
                        "com.puchain.fep.processor.reconciliation..")
                .because("intake.port 仅承载跨模块契约，禁止下沉至 processor 业务实现包")
                .allowEmptyShould(true);
        rule.check(processorClasses);
    }
}
