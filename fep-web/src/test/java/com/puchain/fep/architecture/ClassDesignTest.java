package com.puchain.fep.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Class-level design rules to prevent architectural decay.
 *
 * <ul>
 *   <li>Rule 1: Service/Component constructor params &le; 7 (too many = split)</li>
 *   <li>Rule 2: Controllers must not directly inject Repositories</li>
 * </ul>
 */
@AnalyzeClasses(
    packages = "com.puchain.fep",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ClassDesignTest {

    private static final int MAX_CONSTRUCTOR_PARAMS = 7;

    // TODO: P6e.2 Task 5 will split AuthService (extract LoginVerifier), then remove this exclusion.
    private static final String TEMP_EXCLUDE_AUTH_SERVICE = "com.puchain.fep.web.auth.service.AuthService";

    @ArchTest
    static final ArchRule service_and_component_should_have_limited_dependencies =
        classes()
            .that().areAnnotatedWith(Service.class)
            .or().areAnnotatedWith(Component.class)
            .and().doNotHaveFullyQualifiedName(TEMP_EXCLUDE_AUTH_SERVICE)
            .should(haveConstructorWithAtMostNParameters(MAX_CONSTRUCTOR_PARAMS))
            .because("@Service/@Component with >" + MAX_CONSTRUCTOR_PARAMS
                + " constructor params indicates too many responsibilities — split the class");

    @ArchTest
    static final ArchRule controllers_must_not_directly_depend_on_repositories =
        noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat()
            .areAssignableTo(JpaRepository.class)
            .because("Controller must go through Service layer, not directly inject Repository");

    private static ArchCondition<JavaClass> haveConstructorWithAtMostNParameters(int max) {
        return new ArchCondition<>("have constructors with at most " + max + " parameters") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaConstructor constructor : javaClass.getConstructors()) {
                    int paramCount = constructor.getRawParameterTypes().size();
                    if (paramCount > max) {
                        events.add(SimpleConditionEvent.violated(
                            constructor,
                            String.format("%s has constructor with %d parameters (max %d)",
                                javaClass.getName(), paramCount, max)
                        ));
                    }
                }
            }
        };
    }
}
