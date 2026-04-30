package com.puchain.fep.collector.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * fep-collector 架构边界守护测试（P4 T0 引入）。
 *
 * <p>规则一览：
 * <ul>
 *   <li>R1 — collector 不依赖 transport（队列消费由 P5+ 完成，collector 只负责入队）</li>
 *   <li>R2 — collector 不依赖 converter（XML 序列化 / XSD 校验在 P5+ 由队列消费方完成）</li>
 *   <li>R3 — adapter 子包不依赖 scheduler / assembler（适配器只负责数据采集，不感知调度/组装）</li>
 *   <li>R4 — assembler 子包不依赖 adapter（assembler 接收 CollectionRecord 抽象输入）</li>
 * </ul>
 *
 * <p>R5 见 {@code fep-processor} 模块的 {@code ProcessorArchitectureTest}（intake.port 极简守护）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CollectorArchitectureTest {

    private static JavaClasses collectorClasses;

    @BeforeAll
    static void importClasses() {
        collectorClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.puchain.fep.collector");
    }

    /**
     * R1：collector 不依赖 transport。队列消费/TLQ 发送由 P5+ 完成。
     */
    @Test
    void collectorMustNotDependOnTransport() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.collector..")
                .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.transport..")
                .because("collector 仅负责采集 + 入队，不直接调用 transport（队列消费在 P5+）");
        rule.check(collectorClasses);
    }

    /**
     * R2：collector 不依赖 converter。XML 序列化 / XSD 校验在队列消费侧完成。
     */
    @Test
    void collectorMustNotDependOnConverter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.collector..")
                .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.converter..")
                .because("collector 不参与 XML 序列化 / XSD 校验，避免自反 dep（P5+ 由队列消费方完成）");
        rule.check(collectorClasses);
    }

    /**
     * R3：adapter 子包不依赖 scheduler / assembler。
     *
     * <p>T0 时 adapter / scheduler / assembler 子包均未引入，规则以 trivial PASS 形式存在
     * （allowEmptyShould），作为前置守护防止 T2-T7b 实施时悄悄违反分层。</p>
     */
    @Test
    void adapterMustNotDependOnSchedulerOrAssembler() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.collector.adapter..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.puchain.fep.collector.scheduler..",
                        "com.puchain.fep.collector.assembler..")
                .because("adapter 是底层数据获取，不感知调度 / 组装（反向依赖会破坏分层）")
                .allowEmptyShould(true);
        rule.check(collectorClasses);
    }

    /**
     * R4：assembler 子包不依赖 adapter。
     *
     * <p>T0 时 adapter / assembler 子包均未引入，规则以 trivial PASS 形式存在
     * （allowEmptyShould），作为前置守护防止 T7b 实施时悄悄违反分层。</p>
     */
    @Test
    void assemblerMustNotDependOnAdapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.puchain.fep.collector.assembler..")
                .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.collector.adapter..")
                .because("assembler 接收 CollectionRecord 抽象输入，不感知 adapter 实现")
                .allowEmptyShould(true);
        rule.check(collectorClasses);
    }
}
