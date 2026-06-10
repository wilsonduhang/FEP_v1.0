package com.puchain.fep.security.api;

/**
 * 敏感数据脱敏服务接口。
 *
 * <p>按架构 §680-682 业务规则对敏感字段做确定性脱敏（保留前若干后若干、中间星号掩蔽），
 * 用于 DTO 层展示 / 报文原文查看 / 审计视图（PRD §8.3 + 架构 §1209）。无国密算法（GB 无强制
 * 脱敏算法，按金融脱敏业务规则）；2026-06-07 解禁后 AI Mode A 编写。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface DesensitizeService {

    /**
     * 身份证号脱敏（18 位，保留前 3 后 4，中间星号；架构 §680）。
     *
     * @param idCard 身份证号；null/blank 返回 {@code "N/A"}
     * @return 脱敏串
     */
    String maskIdCard(String idCard);

    /**
     * 银行卡号脱敏（16-19 位，保留前 6 后 4；架构 §681）。
     *
     * @param bankCard 银行卡号；null/blank 返回 {@code "N/A"}
     * @return 脱敏串
     */
    String maskBankCard(String bankCard);

    /**
     * 手机号脱敏（11 位，保留前 3 后 4；架构 §682）。
     *
     * @param phone 手机号；null/blank 返回 {@code "N/A"}
     * @return 脱敏串
     */
    String maskPhone(String phone);

    /**
     * 统一社会信用代码脱敏（18 位，保留后 4；与 LogSanitizer.maskUsci 同主规则，
     * 短串[len≤4]边界改进为全掩不泄漏[LogSanitizer 短串返回原串]）。
     *
     * @param usci USCI；null/blank 返回 {@code "N/A"}
     * @return 脱敏串
     */
    String maskUsci(String usci);
}
