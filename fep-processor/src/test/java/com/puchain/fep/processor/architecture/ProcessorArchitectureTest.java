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
}
