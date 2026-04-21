package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 9100 非实时业务通用转发报文业务体。
 *
 * <p>Used to relay caller-defined non-realtime business payloads between HNDEMP
 * and an accessing institution node. Structurally identical to {@link Forward9000}
 * but kept as an independent POJO (no {@code @XmlSeeAlso} / inheritance) to match
 * the 1:1 XSD-to-class convention adopted in P2a/P2b. The {@code Content} field
 * carries data whose format is agreed between business parties (HNDEMP imposes
 * no schema on it).</p>
 *
 * <p>Fields follow the {@code 9100.xsd} {@code Forward9100} complexType sequence:
 * SrcNodeCode, SrcOrgCode, DesNodeCode, DesOrgCode, BusinessNo (optional), Content.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Forward9100")
@XmlType(propOrder = {"srcNodeCode", "srcOrgCode", "desNodeCode", "desOrgCode", "businessNo", "content"})
public class Forward9100 extends CfxBody {

    @XmlElement(name = "SrcNodeCode", required = true)
    private String srcNodeCode;

    @XmlElement(name = "SrcOrgCode", required = true)
    private String srcOrgCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "DesOrgCode", required = true)
    private String desOrgCode;

    @XmlElement(name = "BusinessNo")
    private String businessNo;

    @XmlElement(name = "Content", required = true)
    private String content;

    /**
     * Returns the source node code (14-digit {@code NodeCode}) that originates the message.
     *
     * @return the source node code
     */
    public String getSrcNodeCode() {
        return srcNodeCode;
    }

    /**
     * Sets the source node code.
     *
     * @param v the source node code
     */
    public void setSrcNodeCode(final String v) {
        this.srcNodeCode = v;
    }

    /**
     * Returns the source organisation's internal code.
     *
     * @return the source org code
     */
    public String getSrcOrgCode() {
        return srcOrgCode;
    }

    /**
     * Sets the source organisation's internal code.
     *
     * @param v the source org code
     */
    public void setSrcOrgCode(final String v) {
        this.srcOrgCode = v;
    }

    /**
     * Returns the destination node code (14-digit {@code NodeCode}) receiving the message.
     *
     * @return the destination node code
     */
    public String getDesNodeCode() {
        return desNodeCode;
    }

    /**
     * Sets the destination node code.
     *
     * @param v the destination node code
     */
    public void setDesNodeCode(final String v) {
        this.desNodeCode = v;
    }

    /**
     * Returns the destination organisation's internal code.
     *
     * @return the destination org code
     */
    public String getDesOrgCode() {
        return desOrgCode;
    }

    /**
     * Sets the destination organisation's internal code.
     *
     * @param v the destination org code
     */
    public void setDesOrgCode(final String v) {
        this.desOrgCode = v;
    }

    /**
     * Returns the optional business category code.
     *
     * @return the business category code, or {@code null} if not set
     */
    public String getBusinessNo() {
        return businessNo;
    }

    /**
     * Sets the optional business category code.
     *
     * @param v the business category code
     */
    public void setBusinessNo(final String v) {
        this.businessNo = v;
    }

    /**
     * Returns the payload content (format is self-negotiated by business parties;
     * HNDEMP does not enforce a schema on it).
     *
     * @return the payload content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the payload content.
     *
     * @param v the payload content
     */
    public void setContent(final String v) {
        this.content = v;
    }
}
