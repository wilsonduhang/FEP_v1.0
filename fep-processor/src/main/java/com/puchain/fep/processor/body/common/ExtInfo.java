package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shared complexType {@code extInfo} — extension/attachment metadata block.
 *
 * <p>Fields follow the XSD {@code extInfo} complexType sequence:
 * ExtJSONFilename, ExtData, ExtGeneralFilename, ExtReserve1-4.
 * All fields are optional.</p>
 *
 * <p>All field types are {@link String}; XSD constraints enforced by
 * {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "extInfo")
@XmlType(propOrder = {
        "extJsonFilename", "extData", "extGeneralFilename",
        "extReserve1", "extReserve2", "extReserve3", "extReserve4"
})
public class ExtInfo extends CfxBody {

    @XmlElement(name = "ExtJSONFilename")
    private String extJsonFilename;

    @XmlElement(name = "ExtData")
    private String extData;

    @XmlElement(name = "ExtGeneralFilename")
    private String extGeneralFilename;

    @XmlElement(name = "ExtReserve1")
    private String extReserve1;

    @XmlElement(name = "ExtReserve2")
    private String extReserve2;

    @XmlElement(name = "ExtReserve3")
    private String extReserve3;

    @XmlElement(name = "ExtReserve4")
    private String extReserve4;

    public String getExtJsonFilename() {
        return extJsonFilename;
    }

    public void setExtJsonFilename(final String v) {
        this.extJsonFilename = v;
    }

    public String getExtData() {
        return extData;
    }

    public void setExtData(final String v) {
        this.extData = v;
    }

    public String getExtGeneralFilename() {
        return extGeneralFilename;
    }

    public void setExtGeneralFilename(final String v) {
        this.extGeneralFilename = v;
    }

    public String getExtReserve1() {
        return extReserve1;
    }

    public void setExtReserve1(final String v) {
        this.extReserve1 = v;
    }

    public String getExtReserve2() {
        return extReserve2;
    }

    public void setExtReserve2(final String v) {
        this.extReserve2 = v;
    }

    public String getExtReserve3() {
        return extReserve3;
    }

    public void setExtReserve3(final String v) {
        this.extReserve3 = v;
    }

    public String getExtReserve4() {
        return extReserve4;
    }

    public void setExtReserve4(final String v) {
        this.extReserve4 = v;
    }
}
