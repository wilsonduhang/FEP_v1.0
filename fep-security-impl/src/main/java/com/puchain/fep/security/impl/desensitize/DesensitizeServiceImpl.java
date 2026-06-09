package com.puchain.fep.security.impl.desensitize;

import com.puchain.fep.security.api.DesensitizeService;

/**
 * {@link DesensitizeService} 实现 — 架构 §680-682 业务脱敏规则。
 *
 * <p>无 Spring stereotype，经 {@code DesensitizeConfiguration @Bean} always-on 注册
 * （脱敏无密钥/无 mock/无 provider 之分；红线 feedback_provider_switch_impl_no_stereotype_bean_registration）。
 * 无状态单例。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DesensitizeServiceImpl implements DesensitizeService {

    private static final String NA = "N/A";
    private static final char MASK = '*';

    /** 身份证：保留前 3 后 4（架构 §680）。 */
    private static final int ID_CARD_PREFIX_KEEP = 3;
    private static final int ID_CARD_SUFFIX_KEEP = 4;

    /** 银行卡：保留前 6 后 4（架构 §681）。 */
    private static final int BANK_CARD_PREFIX_KEEP = 6;
    private static final int BANK_CARD_SUFFIX_KEEP = 4;

    /** 手机号：保留前 3 后 4（架构 §682）。 */
    private static final int PHONE_PREFIX_KEEP = 3;
    private static final int PHONE_SUFFIX_KEEP = 4;

    /** USCI：保留后 4（前 0）。 */
    private static final int USCI_PREFIX_KEEP = 0;
    private static final int USCI_SUFFIX_KEEP = 4;

    @Override
    public String maskIdCard(final String idCard) {
        return maskKeeping(idCard, ID_CARD_PREFIX_KEEP, ID_CARD_SUFFIX_KEEP);
    }

    @Override
    public String maskBankCard(final String bankCard) {
        return maskKeeping(bankCard, BANK_CARD_PREFIX_KEEP, BANK_CARD_SUFFIX_KEEP);
    }

    @Override
    public String maskPhone(final String phone) {
        return maskKeeping(phone, PHONE_PREFIX_KEEP, PHONE_SUFFIX_KEEP);
    }

    @Override
    public String maskUsci(final String usci) {
        return maskKeeping(usci, USCI_PREFIX_KEEP, USCI_SUFFIX_KEEP);
    }

    /**
     * 保留前 {@code prefixKeep} 后 {@code suffixKeep} 字符，中间星号掩蔽。
     *
     * @param value      原值（null/blank → {@code "N/A"}）
     * @param prefixKeep 前保留位数
     * @param suffixKeep 后保留位数
     * @return 脱敏串；长度 ≤ 保留位数和时全掩
     */
    private static String maskKeeping(final String value, final int prefixKeep, final int suffixKeep) {
        if (value == null || value.isBlank()) {
            return NA;
        }
        final int len = value.length();
        if (len <= prefixKeep + suffixKeep) {
            return String.valueOf(MASK).repeat(len);
        }
        final int maskLen = len - prefixKeep - suffixKeep;
        return value.substring(0, prefixKeep)
                + String.valueOf(MASK).repeat(maskLen)
                + value.substring(len - suffixKeep);
    }
}
