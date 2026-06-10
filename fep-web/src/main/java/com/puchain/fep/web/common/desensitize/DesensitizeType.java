package com.puchain.fep.web.common.desensitize;

import com.puchain.fep.security.api.DesensitizeService;

import java.util.function.BiFunction;

/**
 * 脱敏类型 — 路由到 {@link DesensitizeService} 对应方法。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum DesensitizeType {

    /** 身份证号（18 位，保留前 3 后 4）。 */
    ID_CARD(DesensitizeService::maskIdCard),
    /** 银行卡号（16-19 位，保留前 6 后 4）。 */
    BANK_CARD(DesensitizeService::maskBankCard),
    /** 手机号（11 位，保留前 3 后 4）。 */
    PHONE(DesensitizeService::maskPhone),
    /** 统一社会信用代码（18 位，保留后 4）。 */
    USCI(DesensitizeService::maskUsci);

    private final BiFunction<DesensitizeService, String, String> masker;

    DesensitizeType(final BiFunction<DesensitizeService, String, String> masker) {
        this.masker = masker;
    }

    /**
     * 对值应用本类型脱敏。
     *
     * @param service 脱敏服务
     * @param value   原值
     * @return 脱敏串
     */
    public String apply(final DesensitizeService service, final String value) {
        return masker.apply(service, value);
    }
}
