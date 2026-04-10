package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.regex.Pattern;

/**
 * 回执类业务头。参见 PRD v1.3 §3.2.4。
 *
 * <p>继承 {@link RequestBusinessHead} 的 3 字段（SendOrgCode/EntrustDate/TransitionNo
 * 按原值回填），并追加 {@code Result} (5 位数字业务处理结果) 和 {@code AddWord}
 * (≤200 字符处理结果附言)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {"sendOrgCode", "entrustDate", "transitionNo", "result", "addWord"})
public class ResponseBusinessHead extends RequestBusinessHead {

    private static final int RESULT_LENGTH = 5;
    private static final int ADD_WORD_MAX_LENGTH = 200;

    /** 5 位数字业务处理结果码校验模式。 */
    private static final Pattern RESULT_PATTERN =
            Pattern.compile("\\d{" + RESULT_LENGTH + "}");

    private String result;
    private String addWord;

    /**
     * 获取业务处理结果码。
     *
     * @return 业务处理结果，5 位数字
     */
    @XmlElement(name = "Result", required = true)
    public String getResult() {
        return result;
    }

    /**
     * 设置业务处理结果码，null 允许通过，非 null 时必须为 5 位数字。
     *
     * @param v 5 位数字结果码
     */
    public void setResult(final String v) {
        if (v != null && !RESULT_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException("Result 必须为 5 位数字");
        }
        this.result = v;
    }

    /**
     * 获取业务处理结果附言。
     *
     * @return 业务处理结果附言，可选，≤200 字符
     */
    @XmlElement(name = "AddWord")
    public String getAddWord() {
        return addWord;
    }

    /**
     * 设置业务处理结果附言，null 允许通过，非 null 时长度不得超过 200 字符。
     *
     * @param v 附言，最多 200 字符
     */
    public void setAddWord(final String v) {
        if (v != null && v.length() > ADD_WORD_MAX_LENGTH) {
            throw new IllegalArgumentException("AddWord 必须 ≤ 200 字符");
        }
        this.addWord = v;
    }
}
