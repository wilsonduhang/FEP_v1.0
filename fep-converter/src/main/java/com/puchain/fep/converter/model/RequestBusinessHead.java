package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 请求类业务头。参见 PRD v1.3 §3.2.3。
 *
 * <p>3 字段：SendOrgCode / EntrustDate / TransitionNo，全部必填。</p>
 *
 * <p><b>访问策略</b>：{@code @XmlAccessorType(XmlAccessType.PROPERTY)}
 * 保证 JAXB unmarshal 走 setter 路径，入站报文在反序列化阶段即被 setter
 * 校验拒绝。参见 {@link CommonHead} 类级 Javadoc 中 jaxb-runtime 4.x
 * 严格校验要求。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {"sendOrgCode", "entrustDate", "transitionNo"})
public class RequestBusinessHead {

    private static final int ORG_CODE_LENGTH = 14;

    private String sendOrgCode;
    private String entrustDate;
    private String transitionNo;

    /**
     * 获取业务发起方代码。
     *
     * @return 业务发起方 14 位金融机构代码
     */
    @XmlElement(name = "SendOrgCode", required = true)
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
            throw new IllegalArgumentException("SendOrgCode 必须为 14 位");
        }
        this.sendOrgCode = v;
    }

    /**
     * 获取委托日期。
     *
     * @return 委托日期 YYYYMMDD
     */
    @XmlElement(name = "EntrustDate", required = true)
    public String getEntrustDate() {
        return entrustDate;
    }

    /**
     * 设置委托日期，null 允许通过，非 null 时必须为 8 位数字 YYYYMMDD。
     *
     * @param v 委托日期 YYYYMMDD
     */
    public void setEntrustDate(final String v) {
        if (v != null && !v.matches("\\d{8}")) {
            throw new IllegalArgumentException("EntrustDate 必须为 YYYYMMDD");
        }
        this.entrustDate = v;
    }

    /**
     * 获取当日业务流水号。
     *
     * @return 当日业务流水号，8 位数字
     */
    @XmlElement(name = "TransitionNo", required = true)
    public String getTransitionNo() {
        return transitionNo;
    }

    /**
     * 设置当日业务流水号，null 允许通过，非 null 时必须为 8 位数字。
     *
     * @param v 8 位数字流水号
     */
    public void setTransitionNo(final String v) {
        if (v != null && !v.matches("\\d{8}")) {
            throw new IllegalArgumentException("TransitionNo 必须为 8 位数字");
        }
        this.transitionNo = v;
    }
}
