package com.puchain.fep.web.outbound.consumer;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * P5 outbound consumer 包架构约束。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>R1: outbound.consumer 包不得依赖 fep-collector（数据采集层应通过 enqueue 边界解耦）</li>
 *   <li>R2: outbound.consumer 包不得依赖 security.impl（仅可依赖 security.api 接口）</li>
 *   <li>R3: outbound.consumer 包内类命名需以 Outbound 前缀或 Properties / Registry / Composer
 *       / Generator / Test / IntegrationTest 后缀结尾</li>
 *   <li>R4: P5 新建以 OutboundQueue 开头并以 Repository 结尾的类必须落在 outbound.consumer 包
 *       （不约束 P4 已 ship 的 OutboundMessageQueueRepository）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@AnalyzeClasses(packages = "com.puchain.fep")
class OutboundConsumerArchitectureTest {

    @ArchTest
    static final ArchRule R1_outbound_consumer_must_not_depend_on_collector_impl =
        noClasses().that().resideInAPackage("com.puchain.fep.web.outbound.consumer..")
            .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.collector..");

    @ArchTest
    static final ArchRule R2_outbound_consumer_must_not_depend_on_security_impl =
        noClasses().that().resideInAPackage("com.puchain.fep.web.outbound.consumer..")
            .should().dependOnClassesThat().resideInAPackage("com.puchain.fep.security.impl..");

    @ArchTest
    static final ArchRule R3_outbound_consumer_class_naming =
        classes().that().resideInAPackage("com.puchain.fep.web.outbound.consumer..")
            .and().areNotEnums()
            .and().areNotRecords()
            .and().areTopLevelClasses()  // 跳过嵌套配置类（如 *Properties$Retry）
            .should().haveSimpleNameStartingWith("Outbound")
            .orShould().haveSimpleNameEndingWith("Properties")
            .orShould().haveSimpleNameEndingWith("Registry")
            .orShould().haveSimpleNameEndingWith("Composer")
            .orShould().haveSimpleNameEndingWith("Generator")
            .orShould().haveSimpleNameEndingWith("Test")
            .orShould().haveSimpleNameEndingWith("IntegrationTest");

    @ArchTest
    static final ArchRule R4_p5_consumer_repository_only_in_consumer_package =
        // v0.4 修订: 加 P5 前缀过滤，避免误伤 P4 已 ship 的
        // com.puchain.fep.web.outbound.OutboundMessageQueueRepository（不在 consumer 子包，但属 P4 范围）
        // T0 修订: allowEmptyShould(true) 容许 T0 阶段尚未引入 OutboundQueue*Repository
        classes().that().haveSimpleNameEndingWith("Repository")
            .and().haveSimpleNameStartingWith("OutboundQueue")  // 仅约束 P5 新建
            .and().resideInAPackage("com.puchain.fep.web.outbound..")
            .should().resideInAPackage("com.puchain.fep.web.outbound.consumer..")
            .allowEmptyShould(true);
}
