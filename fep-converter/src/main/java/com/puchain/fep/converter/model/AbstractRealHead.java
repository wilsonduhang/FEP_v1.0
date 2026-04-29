package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import java.util.regex.Pattern;

/**
 * 9006 / 9008 节点会话报文业务头共享父类（PRD v1.3 §3.2.2 + §4.5）。
 *
 * <p>对齐 {@code RequestHead} complexType（与 {@link RequestBusinessHead} 同结构，
 * 仅 {@code @XmlRootElement} 元素名不同）：</p>
 *
 * <ul>
 *   <li>{@code SendOrgCode}: 14 字符发起机构代码</li>
 *   <li>{@code EntrustDate}: 8 字符 yyyyMMdd 委托日期</li>
 *   <li>{@code TransitionNo}: 8 字符流水号</li>
 * </ul>
 *
 * <p>字段定义 + 校验规则集中在父类，{@link RealHead9006} / {@link RealHead9008}
 * 仅声明 {@code @XmlRootElement(name=...)} 区分根元素名（P1c T10 R1 closing
 * 抽出，消除两份 100% 重复 POJO）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlTransient
@XmlType(propOrder = {"sendOrgCode", "entrustDate", "transitionNo"})
public abstract class AbstractRealHead {

    private static final int ORG_CODE_LENGTH = 14;

    /** 8 位数字委托日期 YYYYMMDD 校验模式。 */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{8}");

    /** 8 位数字当日业务流水号校验模式。 */
    private static final Pattern TRANSITION_NO_PATTERN = Pattern.compile("\\d{8}");

    @XmlElement(name = "SendOrgCode", required = true)
    private String sendOrgCode;

    @XmlElement(name = "EntrustDate", required = true)
    private String entrustDate;

    @XmlElement(name = "TransitionNo", required = true)
    private String transitionNo;

    /**
     * 获取业务发起方代码。
     *
     * @return 业务发起方 14 位金融机构代码
     */
    public String getSendOrgCode() {
        return sendOrgCode;
    }

    /**
     * 设置业务发起方代码，null 允许通过，非 null 时必须为 14 位。
     *
     * @param v 14 位机构代码
     */
    public void setSendOrgCode(final String v) {
        if (v != null && v.length() != ORG_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "SendOrgCode 必须 " + ORG_CODE_LENGTH + " 字符: " + v);
        }
        this.sendOrgCode = v;
    }

    /**
     * 获取委托日期。
     *
     * @return 委托日期 yyyyMMdd
     */
    public String getEntrustDate() {
        return entrustDate;
    }

    /**
     * 设置委托日期，null 允许通过，非 null 时必须为 8 位数字 yyyyMMdd。
     *
     * @param v 委托日期 yyyyMMdd
     */
    public void setEntrustDate(final String v) {
        if (v != null && !DATE_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException("EntrustDate 必须 yyyyMMdd: " + v);
        }
        this.entrustDate = v;
    }

    /**
     * 获取当日业务流水号。
     *
     * @return 当日业务流水号，8 位数字
     */
    public String getTransitionNo() {
        return transitionNo;
    }

    /**
     * 设置当日业务流水号，null 允许通过，非 null 时必须为 8 位数字。
     *
     * @param v 8 位数字流水号
     */
    public void setTransitionNo(final String v) {
        if (v != null && !TRANSITION_NO_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException("TransitionNo 必须 8 位数字: " + v);
        }
        this.transitionNo = v;
    }
}
