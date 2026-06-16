package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3107 {@link PzCheckQuery3107}
 * and 3108 {@link PzCheckQueryReturn3108} supply chain Body POJOs plus their
 * supplychain support types ({@link HxqyInfo}, {@link PzCheckReturn}).
 *
 * <p>Verifies all 7 main fields per body and the nested list elements
 * roundtrip correctly, ensuring JAXB binding for every nested complexType in
 * 3107.xsd / 3108.xsd.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3107_3108Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    PzCheckQuery3107.class, PzCheckQueryReturn3108.class,
                    HxqyInfo.class, PzCheckReturn.class,
                    ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── PzCheckQuery3107 ─────────────────────────────────────

    @Test
    void pzCheckQuery3107_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(PzCheckQuery3107.class)).isTrue();
    }

    @Test
    void pzCheckQuery3107_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        HxqyInfo hx1 = new HxqyInfo();
        hx1.setHxqyName("湖南某某核心企业A");
        hx1.setHxqyCode("91430100MA00000001");

        HxqyInfo hx2 = new HxqyInfo();
        hx2.setHxqyName("湖南某某核心企业B");
        hx2.setHxqyCode("91430100MA00000002");

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3107附加数据");
        ext.setExtJsonFilename("ext3107.json");

        PzCheckQuery3107 original = new PzCheckQuery3107();
        original.setSerialNo("SN3107-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setCheckDate("20260424");
        original.setHxqyNum("2");
        original.setHxqyInfo(List.of(hx1, hx2));
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<pzCheckQuery3107")
                .contains("<SerialNo>SN3107-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNodeCode>")
                .contains("<CheckDate>20260424</CheckDate>")
                .contains("<hxqyNum>2</hxqyNum>")
                .contains("<hxqyInfo>")
                .contains("<hxqyName>湖南某某核心企业A</hxqyName>")
                .contains("<hxqyCode>91430100MA00000001</hxqyCode>")
                .contains("<hxqyName>湖南某某核心企业B</hxqyName>")
                .contains("<hxqyCode>91430100MA00000002</hxqyCode>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3107附加数据</ExtData>");

        // Field order check (XSD propOrder: SerialNo < CheckDate < hxqyNum < hxqyInfo < ExtInfo)
        int idxSerial = xml.indexOf("<SerialNo>");
        int idxCheckDate = xml.indexOf("<CheckDate>");
        int idxHxqyNum = xml.indexOf("<hxqyNum>");
        int idxHxqyInfo = xml.indexOf("<hxqyInfo>");
        int idxExt = xml.indexOf("<ExtInfo>");
        assertThat(idxSerial).isLessThan(idxCheckDate);
        assertThat(idxCheckDate).isLessThan(idxHxqyNum);
        assertThat(idxHxqyNum).isLessThan(idxHxqyInfo);
        assertThat(idxHxqyInfo).isLessThan(idxExt);

        PzCheckQuery3107 parsed = unmarshal(xml, PzCheckQuery3107.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3107-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getCheckDate()).isEqualTo("20260424");
        assertThat(parsed.getHxqyNum()).isEqualTo("2");

        List<HxqyInfo> parsedHx = parsed.getHxqyInfo();
        assertThat(parsedHx).hasSize(2);
        assertThat(parsedHx.get(0).getHxqyName()).isEqualTo("湖南某某核心企业A");
        assertThat(parsedHx.get(0).getHxqyCode()).isEqualTo("91430100MA00000001");
        assertThat(parsedHx.get(1).getHxqyName()).isEqualTo("湖南某某核心企业B");
        assertThat(parsedHx.get(1).getHxqyCode()).isEqualTo("91430100MA00000002");

        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3107附加数据");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3107.json");
    }

    // ── PzCheckQueryReturn3108 ─────────────────────────────────────

    @Test
    void pzCheckQueryReturn3108_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(PzCheckQueryReturn3108.class)).isTrue();
    }

    @Test
    void pzCheckQueryReturn3108_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        // Two return entries: one with all 7 fields, one omitting optional RetMemo + pzFilename
        PzCheckReturn r1 = new PzCheckReturn();
        r1.setHxqyName("湖南某某核心企业A");
        r1.setHxqyCode("91430100MA00000001");
        r1.setRetCode("90000");
        r1.setRetMemo("核对成功");
        r1.setPzCountAll("128");
        r1.setPzAmtAll("10240000.00");
        r1.setPzFilename("pz-A-20260424.zip");

        PzCheckReturn r2 = new PzCheckReturn();
        r2.setHxqyName("湖南某某核心企业B");
        r2.setHxqyCode("91430100MA00000002");
        r2.setRetCode("90001");
        // RetMemo omitted (optional)
        r2.setPzCountAll("0");
        r2.setPzAmtAll("0.00");
        // pzFilename omitted (optional)

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3108回执扩展");

        PzCheckQueryReturn3108 original = new PzCheckQueryReturn3108();
        original.setSerialNo("SN3108-001");
        original.setSendNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setDesNodeCode("B1001010203");
        original.setCheckDate("20260424");
        original.setHxqyNum("2");
        original.setPzCheckReturn(List.of(r1, r2));
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<pzCheckQueryReturn3108")
                .contains("<SerialNo>SN3108-001</SerialNo>")
                .contains("<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>")
                .contains("<DesNodeCode>B1001010203</DesNodeCode>")
                .contains("<CheckDate>20260424</CheckDate>")
                .contains("<hxqyNum>2</hxqyNum>")
                .contains("<pzCheckReturn>")
                .contains("<RetCode>90000</RetCode>")
                .contains("<RetMemo>核对成功</RetMemo>")
                .contains("<pzCountAll>128</pzCountAll>")
                .contains("<pzAmtAll>10240000.00</pzAmtAll>")
                .contains("<pzFilename>pz-A-20260424.zip</pzFilename>")
                .contains("<RetCode>90001</RetCode>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3108回执扩展</ExtData>");

        PzCheckQueryReturn3108 parsed = unmarshal(xml, PzCheckQueryReturn3108.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3108-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getDesNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getCheckDate()).isEqualTo("20260424");
        assertThat(parsed.getHxqyNum()).isEqualTo("2");

        List<PzCheckReturn> rs = parsed.getPzCheckReturn();
        assertThat(rs).hasSize(2);

        PzCheckReturn p1 = rs.get(0);
        assertThat(p1.getHxqyName()).isEqualTo("湖南某某核心企业A");
        assertThat(p1.getHxqyCode()).isEqualTo("91430100MA00000001");
        assertThat(p1.getRetCode()).isEqualTo("90000");
        assertThat(p1.getRetMemo()).isEqualTo("核对成功");
        assertThat(p1.getPzCountAll()).isEqualTo("128");
        assertThat(p1.getPzAmtAll()).isEqualTo("10240000.00");
        assertThat(p1.getPzFilename()).isEqualTo("pz-A-20260424.zip");

        PzCheckReturn p2 = rs.get(1);
        assertThat(p2.getHxqyName()).isEqualTo("湖南某某核心企业B");
        assertThat(p2.getHxqyCode()).isEqualTo("91430100MA00000002");
        assertThat(p2.getRetCode()).isEqualTo("90001");
        assertThat(p2.getRetMemo()).isNull();
        assertThat(p2.getPzCountAll()).isEqualTo("0");
        assertThat(p2.getPzAmtAll()).isEqualTo("0.00");
        assertThat(p2.getPzFilename()).isNull();

        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3108回执扩展");
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
