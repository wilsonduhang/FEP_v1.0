package com.puchain.fep.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 包结构规则 — 禁止 security.impl 包泄漏到业务代码。
 */
@AnalyzeClasses(
    packages = "com.puchain.fep",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class PackageStructureTest {

    @ArchTest
    static final ArchRule business_code_must_not_depend_on_security_impl =
        noClasses()
            .that().resideInAPackage("com.puchain.fep..")
            .and().resideOutsideOfPackage("com.puchain.fep.security.impl..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.puchain.fep.security.impl..")
            .because("业务代码只能依赖 security.api 接口，不能直接依赖 security.impl 实现 (密钥材料隔离域——2026-06-07 解禁后分层隔离保留)");

    @ArchTest
    static final ArchRule all_classes_in_fep_package =
        classes()
            .that().haveSimpleNameNotEndingWith("Test")
            .and().haveSimpleNameNotEndingWith("IT")
            .and().areNotAnonymousClasses()
            .should().resideInAPackage("com.puchain.fep..")
            .because("所有类必须位于 com.puchain.fep 顶级包下");
}
