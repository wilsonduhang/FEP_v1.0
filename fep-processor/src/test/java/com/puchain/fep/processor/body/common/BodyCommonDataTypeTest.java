package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roundtrip + inheritance tests for shared DataType.xsd common POJOs
 * ({@link QyAccInfo} / {@link PersonInfo} / {@link FileInfo}).
 *
 * <p>Six tests total: 3 JAXB marshal/unmarshal roundtrip + 3 {@link CfxBody}
 * inheritance assertions (Plan P2d-ext T0.5 acceptance criteria #4).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class BodyCommonDataTypeTest {

    @Test
    void qyAccInfo_extendsCfxBody() {
        assertThat(CfxBody.class).isAssignableFrom(QyAccInfo.class);
    }

    @Test
    void qyAccInfo_roundtrip() throws Exception {
        QyAccInfo o = new QyAccInfo();
        o.setAccName("ACC-A");
        o.setAccNumber("6228001234567890");
        o.setAccBankName("某行某支");
        o.setAccBankCode("BANK-001");
        QyAccInfo p = JaxbRoundtripSupport.unmarshal(JaxbRoundtripSupport.marshal(o), QyAccInfo.class);
        assertThat(p.getAccName()).isEqualTo("ACC-A");
        assertThat(p.getAccNumber()).isEqualTo("6228001234567890");
        assertThat(p.getAccBankName()).isEqualTo("某行某支");
        assertThat(p.getAccBankCode()).isEqualTo("BANK-001");
    }

    @Test
    void personInfo_extendsCfxBody() {
        assertThat(CfxBody.class).isAssignableFrom(PersonInfo.class);
    }

    @Test
    void personInfo_roundtrip() throws Exception {
        PersonInfo o = new PersonInfo();
        o.setName("张三");
        o.setCertType("1");
        o.setCertNumber("110000199001011234");
        o.setCertStartDate("2020-01-01");
        o.setCertEndDate("2030-01-01");
        o.setPhone("13800138000");
        o.setPostAddr("北京市");
        o.setMailAddr("zs@example.com");
        PersonInfo p = JaxbRoundtripSupport.unmarshal(JaxbRoundtripSupport.marshal(o), PersonInfo.class);
        assertThat(p.getName()).isEqualTo("张三");
        assertThat(p.getCertType()).isEqualTo("1");
        assertThat(p.getCertNumber()).isEqualTo("110000199001011234");
        assertThat(p.getCertStartDate()).isEqualTo("2020-01-01");
        assertThat(p.getCertEndDate()).isEqualTo("2030-01-01");
        assertThat(p.getPhone()).isEqualTo("13800138000");
        assertThat(p.getPostAddr()).isEqualTo("北京市");
        assertThat(p.getMailAddr()).isEqualTo("zs@example.com");
    }

    @Test
    void fileInfo_extendsCfxBody() {
        assertThat(CfxBody.class).isAssignableFrom(FileInfo.class);
    }

    @Test
    void fileInfo_roundtrip() throws Exception {
        FileInfo o = new FileInfo();
        o.setFileType("1");
        o.setFilename("contract.pdf");
        o.setFileMemo("主合同");
        FileInfo p = JaxbRoundtripSupport.unmarshal(JaxbRoundtripSupport.marshal(o), FileInfo.class);
        assertThat(p.getFileType()).isEqualTo("1");
        assertThat(p.getFilename()).isEqualTo("contract.pdf");
        assertThat(p.getFileMemo()).isEqualTo("主合同");
    }
}
