package com.puchain.fep.web.callback.alert.sms;

/**
 * SMS 网关抽象：屏蔽具体短信服务商（阿里云/腾讯云/自建）。本 Plan 仅提供
 * {@code CallbackLoggingSmsGateway} log 桩；真实网关 SDK 接入由独立 Plan 完成（⛔ 网关密钥
 * 走配置注入禁硬编码）。参见 PRD v1.3 §5.5.3（FR-INFRA-CALLBACK-ALERT）。
 *
 * <p><b>bean 名约定</b>：log 桩 {@code CallbackLoggingSmsGateway} 标
 * {@code @ConditionalOnMissingBean(name = "realSmsGateway")}。将来真实网关实现<b>必须</b>以 bean 名
 * {@code realSmsGateway} 注册（{@code @Bean CallbackSmsGateway realSmsGateway()} 或
 * {@code @Component("realSmsGateway")}），否则与 log 桩冲突（NoUniqueBeanDefinition）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface CallbackSmsGateway {

    /**
     * 发送短信。
     *
     * @param phone   收件手机号
     * @param content 短信内容
     */
    void send(String phone, String content);
}
