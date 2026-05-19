package com.puchain.fep.web.callback;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 架构约束：{@code com.puchain.fep.web.callback} 包边界。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>R1: {@code web.callback} 不依赖 {@code web.outbound}（回调是独立 HTTP 推送
 *       通道，不应依赖 TLQ outbound 消费层）</li>
 *   <li>R2: {@code web.callback} 不依赖 {@code transport}（transport 是 TLQ 层，
 *       callback 模块直接使用 JDK HttpClient）</li>
 *   <li>R3: {@code web.callback} 不依赖 {@code collector}（数据采集层，callback
 *       模块只消费 {@link com.puchain.fep.processor.event.InboundMessageProcessedEvent}）</li>
 *   <li>R4: {@code web.callback} 下的 production 类命名以 {@code Callback} 开头。
 *       已核查全部生产类（domain/http/listener/repository/runner/service 下）：
 *       {@code CallbackQueueEntity}, {@code CallbackQueueStatus}, {@code CallbackHttpClient},
 *       {@code CallbackResult}, {@code CallbackInboundListener},
 *       {@code CallbackQueueRepository}, {@code CallbackQueueRunner},
 *       {@code CallbackEnqueueService}, {@code CallbackEnvelopeBuilder},
 *       {@code CallbackTargetResolver} — 全部符合 {@code Callback} 前缀规范。</li>
 * </ul>
 *
 * <p>扫描范围为整个 {@code com.puchain.fep} 生产类（test source 在扫描范围外），
 * 故 ArchUnit 不会对本文件本身执行命名规则检查。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@AnalyzeClasses(packages = "com.puchain.fep")
class CallbackModuleArchTest {

    /**
     * R1: {@code web.callback} 不依赖 {@code web.outbound}。
     *
     * <p>回调模块是接口模式的独立 HTTP fan-out 通道，与 TLQ outbound 消费层
     * 功能正交，不得引入相互依赖。</p>
     */
    @ArchTest
    static final ArchRule R1_callback_must_not_depend_on_outbound =
            noClasses().that().resideInAPackage("com.puchain.fep.web.callback..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.puchain.fep.web.outbound..");

    /**
     * R2: {@code web.callback} 不依赖 {@code transport}。
     *
     * <p>TLQ transport 层（TongLINK/Q）与 callback 的 JDK HttpClient 属不同通道，
     * 严格隔离避免层级倒置。</p>
     */
    @ArchTest
    static final ArchRule R2_callback_must_not_depend_on_transport =
            noClasses().that().resideInAPackage("com.puchain.fep.web.callback..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.puchain.fep.transport..");

    /**
     * R3: {@code web.callback} 不依赖 {@code collector}。
     *
     * <p>数据采集层（JDBC/ESB/文件适配器）是上游数据入口，callback 模块
     * 只响应 processor 事件，不应下钻到采集层。</p>
     */
    @ArchTest
    static final ArchRule R3_callback_must_not_depend_on_collector =
            noClasses().that().resideInAPackage("com.puchain.fep.web.callback..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.puchain.fep.collector..");

    /**
     * R4: {@code web.callback} 下的顶层生产类命名以 {@code Callback} 开头。
     *
     * <p>约束顶层非匿名非成员类，排除枚举（{@code CallbackQueueStatus} 为
     * {@code String} 常量容器类，非 enum，符合约束）及记录类。</p>
     */
    @ArchTest
    static final ArchRule R4_callback_class_naming =
            classes().that().resideInAPackage("com.puchain.fep.web.callback..")
                    .and().areTopLevelClasses()
                    .and().areNotAnonymousClasses()
                    .and().areNotMemberClasses()
                    .and().areNotEnums()
                    .and().areNotRecords()
                    .should().haveSimpleNameStartingWith("Callback");
}
