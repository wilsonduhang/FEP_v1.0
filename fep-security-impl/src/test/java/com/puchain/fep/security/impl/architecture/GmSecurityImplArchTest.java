package com.puchain.fep.security.impl.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * fep-security-impl 国密实现架构守护：BouncyCastle 仅本模块内用，不反向泄漏到
 * {@code security.api} / {@code common}（impl 内部自守护）。
 *
 * <p>跨业务模块（converter/outbound/callback）的 BC 禁令由各自模块 ArchTest 强制
 * （ConverterArchitectureTest / OutboundConsumerArchitectureTest R2 / CallbackModuleArchTest
 * R5/R8），引入 impl 后立即生效。</p>
 */
class GmSecurityImplArchTest {

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.puchain.fep");

    @Test
    void onlySecurityImplMayDependOnBouncyCastle() {
        noClasses().that().resideOutsideOfPackage("com.puchain.fep.security.impl..")
                .should().dependOnClassesThat().resideInAPackage("org.bouncycastle..")
                .check(classes);
    }
}
