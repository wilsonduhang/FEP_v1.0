package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shared complexType {@code RiskRate} — risk rate with optional memo.
 *
 * <p>Fields follow the XSD {@code RiskRate} complexType sequence:
 * Rate (required), RateMemo (optional).</p>
 *
 * <p>All field types are {@link String}; XSD constraints enforced by
 * {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "RiskRate")
@XmlType(propOrder = {"rate", "rateMemo"})
public class RiskRate extends CfxBody {

    @XmlElement(name = "Rate", required = true)
    private String rate;

    @XmlElement(name = "RateMemo")
    private String rateMemo;

    /**
     * @return risk rate value
     */
    public String getRate() {
        return rate;
    }

    /**
     * @param v risk rate value
     */
    public void setRate(final String v) {
        this.rate = v;
    }

    /**
     * @return risk rate memo, may be null
     */
    public String getRateMemo() {
        return rateMemo;
    }

    /**
     * @param v risk rate memo
     */
    public void setRateMemo(final String v) {
        this.rateMemo = v;
    }
}
