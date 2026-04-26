package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Shared complexType {@code FileInfo} (DataType.xsd) — file attachment metadata block.
 *
 * <p>Fields follow the XSD {@code FileInfo} complexType sequence:
 * FileType, Filename (required), FileMemo (3 fields).</p>
 *
 * <p>Used as a nested file-attachment block by supply-chain messages such as
 * 3102 / 3105 (contract / invoice attachment metadata).</p>
 *
 * <p>All field types are {@link String}; XSD constraints enforced by
 * {@link com.puchain.fep.processor.validation.XsdValidator}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "FileInfo")
@XmlType(propOrder = {"fileType", "filename", "fileMemo"})
public class FileInfo extends CfxBody {

    @XmlElement(name = "FileType")
    private String fileType;

    @XmlElement(name = "Filename", required = true)
    private String filename;

    @XmlElement(name = "FileMemo")
    private String fileMemo;

    public String getFileType() {
        return fileType;
    }

    public void setFileType(final String v) {
        this.fileType = v;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String v) {
        this.filename = v;
    }

    public String getFileMemo() {
        return fileMemo;
    }

    public void setFileMemo(final String v) {
        this.fileMemo = v;
    }
}
