package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.PzInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3000 {@link DzpzInfo3000}
 * supply chain Body POJO.
 *
 * <p>Covers the three canonical cases mandated by Plan B v0.4 T3:
 * <ol>
 *   <li>full payload with all 6 top-level fields populated</li>
 *   <li>optional {@code pzInfo} / {@code ExtInfo} omitted</li>
 *   <li>nested {@link PzInfo} scalar fields preserved roundtrip</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3000Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    DzpzInfo3000.class, PzInfo.class, ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    void dzpzInfo3000_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(DzpzInfo3000.class)).isTrue();
    }

    @Test
    void dzpzInfo3000_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        DzpzInfo3000 original = new DzpzInfo3000();
        original.setSerialNo("SN3000-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode("A1000143000104");
        original.setApplyMode("01");

        PzInfo pz = new PzInfo();
        pz.setPlatShortName("某平台简称");
        pz.setPlatCode("PLAT001");
        pz.setExternalPlat("0");
        pz.setHxqyName("核心企业A");
        pz.setHxqyCode("91430000HXQYCODE01");
        pz.setPzNo("PZ20260508000001");
        pz.setPzClass("1");
        pz.setPzFunction("01");
        pz.setKlzrfName("开立责任方");
        pz.setKlzrfCode("91430000KLZRFCODE0");
        pz.setJsqyName("接收企业B");
        pz.setJsqyCode("91430000JSQYCODE01");
        pz.setJsqyPlatNo("JSQYPLAT001");
        pz.setPzAmt("100000.00");
        pz.setPzStartDate("20260508");
        pz.setPzEndDate("20270508");
        pz.setPzState("01");
        pz.setPzrzState("01");
        pz.setPzFlowNum("0");
        original.setPzInfo(pz);

        ExtInfo ext = new ExtInfo();
        ext.setExtJsonFilename("ext3000.json");
        ext.setExtData("3000附加数据");
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<DzpzInfo3000")
                .contains("<SerialNo>SN3000-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<ApplyMode>01</ApplyMode>")
                .contains("<pzInfo>")
                .contains("<PlatShortName>某平台简称</PlatShortName>")
                .contains("<pzNo>PZ20260508000001</pzNo>")
                .contains("<pzAmt>100000.00</pzAmt>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3000附加数据</ExtData>");

        DzpzInfo3000 parsed = unmarshal(xml, DzpzInfo3000.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3000-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getApplyMode()).isEqualTo("01");
        assertThat(parsed.getPzInfo()).isNotNull();
        assertThat(parsed.getPzInfo().getPzNo()).isEqualTo("PZ20260508000001");
        assertThat(parsed.getPzInfo().getPzAmt()).isEqualTo("100000.00");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3000.json");
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3000附加数据");
    }

    @Test
    void dzpzInfo3000_optionalFieldsOmitted_shouldMarshalWithoutThem() throws Exception {
        DzpzInfo3000 original = new DzpzInfo3000();
        original.setSerialNo("SN3000-002");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode("A1000143000104");
        original.setApplyMode("02");
        // pzInfo + ExtInfo deliberately null

        String xml = marshal(original);
        assertThat(xml)
                .contains("<SerialNo>SN3000-002</SerialNo>")
                .contains("<ApplyMode>02</ApplyMode>")
                .doesNotContain("<pzInfo>")
                .doesNotContain("<ExtInfo>");

        DzpzInfo3000 parsed = unmarshal(xml, DzpzInfo3000.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3000-002");
        assertThat(parsed.getApplyMode()).isEqualTo("02");
        assertThat(parsed.getPzInfo()).isNull();
        assertThat(parsed.getExtInfo()).isNull();
    }

    @Test
    void dzpzInfo3000_fieldOrder_shouldMatchXsdSequence() throws Exception {
        DzpzInfo3000 original = new DzpzInfo3000();
        original.setSerialNo("SN");
        original.setSendNodeCode("SNC");
        original.setDesNodeCode("DNC");
        original.setApplyMode("01");

        String xml = marshal(original);
        int idxSerial = xml.indexOf("<SerialNo>");
        int idxSend = xml.indexOf("<SendNodeCode>");
        int idxDes = xml.indexOf("<DesNodeCode>");
        int idxApply = xml.indexOf("<ApplyMode>");

        // XSD propOrder: SerialNo < SendNodeCode < DesNodeCode < ApplyMode
        assertThat(idxSerial).isLessThan(idxSend);
        assertThat(idxSend).isLessThan(idxDes);
        assertThat(idxDes).isLessThan(idxApply);
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
