package com.puchain.fep.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 命名规范 — Spring 组件类名必须遵循约定。
 */
@AnalyzeClasses(
    packages = "com.puchain.fep",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class NamingConventionTest {

    @ArchTest
    static final ArchRule configuration_classes_name_ends_with_configuration =
        classes()
            .that().areAnnotatedWith(Configuration.class)
            .should().haveSimpleNameEndingWith("Configuration")
            .because("@Configuration 类名必须以 Configuration 结尾");

    @ArchTest
    static final ArchRule service_classes_name_ends_with_service =
        classes()
            .that().areAnnotatedWith(Service.class)
            .should().haveSimpleNameEndingWith("Service")
            .because("@Service 类名必须以 Service 结尾");
}
