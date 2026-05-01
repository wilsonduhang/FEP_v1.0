package com.puchain.fep.web.outbound.xml;

import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Objects;

/**
 * JAXB wrapper that marshals the cross-module {@link OutboundHeadFields} record
 * into the {@code message_head_xml} column of {@code outbound_message_queue}
 * (P4 T7a).
 *
 * <p>The source record cannot be annotated directly because it lives in
 * {@code fep-processor.intake.port}, a deliberately minimal package whose only
 * dependency is {@link Objects} (cross-module contract surface, ArchUnit-guarded).
 * Wrapping in fep-web keeps the JAXB annotation surface in the Adapter layer
 * where it belongs.</p>
 *
 * <p>The element names match the wire-level conventions established by
 * {@code fep-converter.RequestBusinessHead} so the future P5+ consumer that
 * re-parses this XML can reuse the same JAXB layout.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlRootElement(name = "OutboundHead")
@XmlAccessorType(XmlAccessType.FIELD)
public class OutboundHeadFieldsXml {

    @XmlElement(name = "SendOrgCode")
    private String sendOrgCode;

    @XmlElement(name = "EntrustDate")
    private String entrustDate;

    @XmlElement(name = "TransitionNo")
    private String transitionNo;

    /**
     * No-arg constructor required by JAXB.
     */
    public OutboundHeadFieldsXml() {
        // JAXB
    }

    /**
     * Builds a wrapper from the cross-module record. All three fields are
     * already null-checked by the source {@link OutboundHeadFields} compact
     * constructor, so no further null-guards are needed here.
     *
     * @param source the {@link OutboundHeadFields} to copy from (non-null)
     */
    public OutboundHeadFieldsXml(final OutboundHeadFields source) {
        Objects.requireNonNull(source, "source");
        this.sendOrgCode = source.sendOrgCode();
        this.entrustDate = source.entrustDate();
        this.transitionNo = source.transitionNo();
    }

    public String getSendOrgCode() {
        return sendOrgCode;
    }

    public void setSendOrgCode(final String sendOrgCode) {
        this.sendOrgCode = sendOrgCode;
    }

    public String getEntrustDate() {
        return entrustDate;
    }

    public void setEntrustDate(final String entrustDate) {
        this.entrustDate = entrustDate;
    }

    public String getTransitionNo() {
        return transitionNo;
    }

    public void setTransitionNo(final String transitionNo) {
        this.transitionNo = transitionNo;
    }
}
