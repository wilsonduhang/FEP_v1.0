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

    /**
     * R5: {@code credential.crypto}（凭证 SM4 加解密门面）不依赖 {@code security.impl}。
     *
     * <p>Phase 2b 凭证子系统经 {@code CallbackCredentialEncryptionFacade} 仅调用
     * {@code security.api}（{@code CryptoService}/{@code KeyService} 接口），严守 CLAUDE.md
     * ⛔ 安全分层隔离：{@code security.impl}（国密实现，密钥材料隔离域——2026-06-07 解禁后分层隔离保留）对应用层不可见。
     * 当前 {@code security.impl} 尚未引入，本规则为前向守护，引入时立即生效。</p>
     */
    @ArchTest
    static final ArchRule R5_credential_crypto_must_not_depend_on_security_impl =
            noClasses().that().resideInAPackage("com.puchain.fep.web.callback.credential.crypto..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.puchain.fep.security.impl..");

    /**
     * R6: {@code callback.notification}（告警监听 + 站内通知）不依赖 {@code callback.credential}
     * 或 {@code callback.reaper}。
     *
     * <p>通知子系统经事件解耦（{@code @EventListener CallbackDeadLetterEvent}），与凭证子系统、
     * reaper 回收子系统功能正交，不得引入横向耦合，保持各子域可独立演进。</p>
     */
    @ArchTest
    static final ArchRule R6_notification_must_not_depend_on_credential_or_reaper =
            noClasses().that().resideInAPackage("com.puchain.fep.web.callback.notification..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.puchain.fep.web.callback.credential..",
                            "com.puchain.fep.web.callback.reaper..");

    /**
     * R7: {@code callback} 下的 {@code @RestController} 不直接依赖 repository 层。
     *
     * <p>控制器须经 service 层访问持久化，禁止直连 repository（DLQ/凭证/通知三控制器均经
     * {@code CallbackReplayService}/{@code CallbackCredentialAdminService}/
     * {@code CallbackNotificationService} 委派），保持分层职责。</p>
     */
    @ArchTest
    static final ArchRule R7_callback_controllers_must_not_depend_on_repository =
            noClasses().that().resideInAPackage("com.puchain.fep.web.callback..")
                    .and().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().dependOnClassesThat()
                    .resideInAPackage("com.puchain.fep.web.callback..repository..");

    /**
     * R8: {@code credential.crypto}（含 {@code CallbackCredentialEncryptionFacade}）仅被
     * {@code callback.credential} 子包内的类依赖。
     *
     * <p>SM4 加解密门面是凭证子系统内部实现细节，不得泄漏到 DLQ/通知/reaper 等其他子域；
     * 当前仅 {@code CallbackCredentialResolver} / {@code CallbackCredentialAdminService}
     * （均 {@code credential.service}）使用。</p>
     */
    @ArchTest
    static final ArchRule R8_credential_crypto_only_used_within_credential =
            classes().that().resideInAPackage("com.puchain.fep.web.callback.credential.crypto..")
                    .should().onlyHaveDependentClassesThat()
                    .resideInAnyPackage("com.puchain.fep.web.callback.credential..");

    /**
     * R9: {@code callback.alert}（统一告警引擎 + 渠道）不依赖 {@code callback.credential}
     * 或 {@code callback.reaper}。
     *
     * <p>告警子系统经事件解耦（{@code @EventListener CallbackDeadLetterEvent}），与凭证、reaper
     * 子系统功能正交，不得横向耦合（镜像 R6）。</p>
     */
    @ArchTest
    static final ArchRule R9_alert_must_not_depend_on_credential_or_reaper =
            noClasses().that().resideInAPackage("com.puchain.fep.web.callback.alert..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.puchain.fep.web.callback.credential..",
                            "com.puchain.fep.web.callback.reaper..");
}
