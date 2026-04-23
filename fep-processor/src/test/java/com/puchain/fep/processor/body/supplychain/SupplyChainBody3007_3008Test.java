package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3007 {@link InvoCheckQuery3007}
 * and 3008 {@link InvoCheckReturn3008} supply chain Body POJOs.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3007_3008Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    InvoCheckQuery3007.class, InvoCheckReturn3008.class,
                    ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── InvoCheckQuery3007 ─────────────────────────────────────

    @Test
    void invoCheckQuery3007_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(InvoCheckQuery3007.class)).isTrue();
    }

    @Test
    void invoCheckQuery3007_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        InvoCheckQuery3007 original = new InvoCheckQuery3007();
        original.setSerialNo("SN3007-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode("A1000143000104");
        original.setInvoCode("011001234567");
        original.setInvoNum("044001234567890123");
        original.setCheckCode("ABCDEF");
        original.setInvoAmtTax("10000.00");
        original.setInvoAmt("9433.96");
        original.setInvoDate("20260423");
        original.setYwKeyValue("BIZKEY-2026042300001");
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3007附加数据");
        ext.setExtJsonFilename("ext3007.json");
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<InvoCheckQuery3007")
                .contains("<SerialNo>SN3007-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<InvoCode>011001234567</InvoCode>")
                .contains("<InvoNum>044001234567890123</InvoNum>")
                .contains("<CheckCode>ABCDEF</CheckCode>")
                .contains("<InvoAmtTax>10000.00</InvoAmtTax>")
                .contains("<InvoAmt>9433.96</InvoAmt>")
                .contains("<InvoDate>20260423</InvoDate>")
                .contains("<ywKeyValue>BIZKEY-2026042300001</ywKeyValue>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3007附加数据</ExtData>");

        InvoCheckQuery3007 parsed = unmarshal(xml, InvoCheckQuery3007.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3007-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getInvoCode()).isEqualTo("011001234567");
        assertThat(parsed.getInvoNum()).isEqualTo("044001234567890123");
        assertThat(parsed.getCheckCode()).isEqualTo("ABCDEF");
        assertThat(parsed.getInvoAmtTax()).isEqualTo("10000.00");
        assertThat(parsed.getInvoAmt()).isEqualTo("9433.96");
        assertThat(parsed.getInvoDate()).isEqualTo("20260423");
        assertThat(parsed.getYwKeyValue()).isEqualTo("BIZKEY-2026042300001");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3007附加数据");
    }

    @Test
    void invoCheckQuery3007_fieldOrder_shouldMatchXsdSequence() throws Exception {
        InvoCheckQuery3007 original = new InvoCheckQuery3007();
        original.setSerialNo("SN");
        original.setSendNodeCode("SNC");
        original.setDesNodeCode("DNC");
        original.setInvoCode("IC");
        original.setInvoNum("IN");
        original.setInvoAmt("1.00");
        original.setInvoDate("20260423");

        String xml = marshal(original);
        int idxSerial = xml.indexOf("<SerialNo>");
        int idxInvoCode = xml.indexOf("<InvoCode>");
        int idxInvoNum = xml.indexOf("<InvoNum>");
        int idxInvoAmt = xml.indexOf("<InvoAmt>");

        // XSD propOrder: SerialNo < InvoCode < InvoNum < InvoAmt
        assertThat(idxSerial).isLessThan(idxInvoCode);
        assertThat(idxInvoCode).isLessThan(idxInvoNum);
        assertThat(idxInvoNum).isLessThan(idxInvoAmt);
    }

    // ── InvoCheckReturn3008 ─────────────────────────────────────

    @Test
    void invoCheckReturn3008_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(InvoCheckReturn3008.class)).isTrue();
    }

    @Test
    void invoCheckReturn3008_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        InvoCheckReturn3008 original = new InvoCheckReturn3008();
        original.setSerialNo("SN3008-001");
        original.setSendNodeCode("A1000143000104");
        original.setDesNodeCode("B1001010203");
        original.setInvoCheckReturnCode("0");
        original.setInvoCheckReturnMemo("核验通过");
        original.setKpName("湖南某某销售公司");
        original.setKpCode("91430000123456789X");
        original.setSpName("湖南某某采购公司");

        String xml = marshal(original);
        assertThat(xml)
                .contains("<InvoCheckReturn3008")
                .contains("<SerialNo>SN3008-001</SerialNo>")
                .contains("<InvoCheckReturnCode>0</InvoCheckReturnCode>")
                .contains("<InvoCheckReturnMemo>核验通过</InvoCheckReturnMemo>")
                .contains("<kpName>湖南某某销售公司</kpName>")
                .contains("<kpCode>91430000123456789X</kpCode>")
                .contains("<spName>湖南某某采购公司</spName>");

        InvoCheckReturn3008 parsed = unmarshal(xml, InvoCheckReturn3008.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3008-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getDesNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getInvoCheckReturnCode()).isEqualTo("0");
        assertThat(parsed.getInvoCheckReturnMemo()).isEqualTo("核验通过");
        assertThat(parsed.getKpName()).isEqualTo("湖南某某销售公司");
        assertThat(parsed.getKpCode()).isEqualTo("91430000123456789X");
        assertThat(parsed.getSpName()).isEqualTo("湖南某某采购公司");
    }

    @Test
    void invoCheckReturn3008_optionalFieldsOmitted_shouldMarshalWithoutThem() throws Exception {
        InvoCheckReturn3008 original = new InvoCheckReturn3008();
        original.setSerialNo("SN");
        original.setSendNodeCode("SNC");
        original.setDesNodeCode("DNC");
        original.setInvoCheckReturnCode("0");
        original.setKpName("KP");
        original.setKpCode("KPC");
        original.setSpName("SP");
        // spCode / InvoFilename / ExtInfo / other optionals kept null

        String xml = marshal(original);
        assertThat(xml)
                .doesNotContain("<spCode>")
                .doesNotContain("<InvoFilename>")
                .doesNotContain("<ExtInfo>")
                .doesNotContain("<InvoCheckReturnMemo>");
    }

    // ── helpers ──────────────────────────────────────────────

    /**
     * Marshal with the shared JAXBContext that includes all complex types.
     */
    private <T> String marshal(final T instance) throws Exception {
        Marshaller marshaller = CTX.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(instance, writer);
        return writer.toString();
    }

    /**
     * Unmarshal with the shared JAXBContext.
     */
    @SuppressWarnings("unchecked")
    private <T> T unmarshal(final String xml, final Class<T> type) throws Exception {
        Unmarshaller unmarshaller = CTX.createUnmarshaller();
        return (T) unmarshaller.unmarshal(new StringReader(xml));
    }
}
