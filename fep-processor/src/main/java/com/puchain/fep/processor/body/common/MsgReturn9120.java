package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 9120 非实时业务通用应答报文业务体。
 *
 * <p>When HNDEMP receives a non-realtime request (e.g. 3001), it immediately
 * sends back a 9120 ACK containing the original message number.</p>
 *
 * <p>Fields follow the {@code 9120.xsd} {@code MsgReturn9120} complexType sequence:
 * OriMsgNo (required), Debug (optional).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MsgReturn9120")
@XmlType(propOrder = {"oriMsgNo", "debug"})
public class MsgReturn9120 extends CfxBody {

    @XmlElement(name = "OriMsgNo", required = true)
    private String oriMsgNo;

    @XmlElement(name = "Debug")
    private String debug;

    /**
     * Returns the original message number this ACK references (e.g. "3001").
     *
     * @return the original message number
     */
    public String getOriMsgNo() {
        return oriMsgNo;
    }

    /**
     * Sets the original message number this ACK references.
     *
     * @param v the original message number
     */
    public void setOriMsgNo(final String v) {
        this.oriMsgNo = v;
    }

    /**
     * Returns the optional debug information.
     *
     * @return debug info, or {@code null} if not set
     */
    public String getDebug() {
        return debug;
    }

    /**
     * Sets the optional debug information.
     *
     * @param v debug info
     */
    public void setDebug(final String v) {
        this.debug = v;
    }
}
