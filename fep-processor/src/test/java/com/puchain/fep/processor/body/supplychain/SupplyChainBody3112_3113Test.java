package com.puchain.fep.processor.body.supplychain;

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
 * JAXB marshal/unmarshal roundtrip tests for 3112 {@link HxqyCreditAmt3112}
 * and 3113 {@link HxqyCreditAmt3113} supply chain Body POJOs plus their
 * supplychain support types ({@link CreditInfo}, {@link CreditInfoBank}).
 *
 * <p>Verifies all 7 main fields per body, the nested list elements, and the
 * deeply nested {@link CreditInfoBank} list inside {@link CreditInfo} all
 * roundtrip correctly. Also confirms reuse of T4's shared {@link HxqyInfo}
 * support type by 3112 (no new {@code HxqyInfo3112} created).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3112_3113Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    HxqyCreditAmt3112.class, HxqyCreditAmt3113.class,
                    HxqyInfo.class, CreditInfo.class, CreditInfoBank.class,
                    ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── HxqyCreditAmt3112 ─────────────────────────────────────

    @Test
    void hxqyCreditAmt3112_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(HxqyCreditAmt3112.class)).isTrue();
    }

    @Test
    void hxqyCreditAmt3112_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        // Reuse T4's shared HxqyInfo (hxqyName + hxqyCode); no HxqyInfo3112 created.
        HxqyInfo hx1 = new HxqyInfo();
        hx1.setHxqyName("湖南某某核心企业A");
        hx1.setHxqyCode("91430100MA00000001");

        HxqyInfo hx2 = new HxqyInfo();
        hx2.setHxqyName("湖南某某核心企业B");
        hx2.setHxqyCode("91430100MA00000002");

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3112附加数据");
        ext.setExtJsonFilename("ext3112.json");

        HxqyCreditAmt3112 original = new HxqyCreditAmt3112();
        original.setSerialNo("SN3112-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode("A1000143000104");
        original.setQueryDate("20260424");
        original.setHxqyInfoNum("2");
        original.setHxqyInfo(List.of(hx1, hx2));
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<hxqyCreditAmt3112")
                .contains("<SerialNo>SN3112-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<QueryDate>20260424</QueryDate>")
                .contains("<hxqyInfoNum>2</hxqyInfoNum>")
                .contains("<hxqyInfo>")
                .contains("<hxqyName>湖南某某核心企业A</hxqyName>")
                .contains("<hxqyCode>91430100MA00000001</hxqyCode>")
                .contains("<hxqyName>湖南某某核心企业B</hxqyName>")
                .contains("<hxqyCode>91430100MA00000002</hxqyCode>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3112附加数据</ExtData>");

        // Field order check (XSD propOrder: SerialNo < QueryDate < hxqyInfoNum < hxqyInfo < ExtInfo)
        int idxSerial = xml.indexOf("<SerialNo>");
        int idxQueryDate = xml.indexOf("<QueryDate>");
        int idxHxqyInfoNum = xml.indexOf("<hxqyInfoNum>");
        int idxHxqyInfo = xml.indexOf("<hxqyInfo>");
        int idxExt = xml.indexOf("<ExtInfo>");
        assertThat(idxSerial).isLessThan(idxQueryDate);
        assertThat(idxQueryDate).isLessThan(idxHxqyInfoNum);
        assertThat(idxHxqyInfoNum).isLessThan(idxHxqyInfo);
        assertThat(idxHxqyInfo).isLessThan(idxExt);

        HxqyCreditAmt3112 parsed = unmarshal(xml, HxqyCreditAmt3112.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3112-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getQueryDate()).isEqualTo("20260424");
        assertThat(parsed.getHxqyInfoNum()).isEqualTo("2");

        List<HxqyInfo> parsedHx = parsed.getHxqyInfo();
        assertThat(parsedHx).hasSize(2);
        assertThat(parsedHx.get(0).getHxqyName()).isEqualTo("湖南某某核心企业A");
        assertThat(parsedHx.get(0).getHxqyCode()).isEqualTo("91430100MA00000001");
        assertThat(parsedHx.get(1).getHxqyName()).isEqualTo("湖南某某核心企业B");
        assertThat(parsedHx.get(1).getHxqyCode()).isEqualTo("91430100MA00000002");

        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3112附加数据");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3112.json");
    }

    // ── HxqyCreditAmt3113 ─────────────────────────────────────

    @Test
    void hxqyCreditAmt3113_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(HxqyCreditAmt3113.class)).isTrue();
    }

    @Test
    void hxqyCreditAmt3113_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        // Bank credit details for entry 1 (two banks); entry 2 has no bank details.
        CreditInfoBank bank1 = new CreditInfoBank();
        bank1.setBankNodeCode("B1001010203");
        bank1.setBankName("某某银行湖南分行");
        bank1.setSxAmt("50000000.00");
        bank1.setSxBalance("18000000.00");
        bank1.setQueryReturnTime("20260424103500");

        CreditInfoBank bank2 = new CreditInfoBank();
        bank2.setBankNodeCode("B1001020304");
        bank2.setBankName("另一家银行湖南分行");
        bank2.setSxAmt("30000000.00");
        bank2.setSxBalance("12500000.50");
        bank2.setQueryReturnTime("20260424103530");

        // Entry 1: success, with RetMemo and 2 bank details
        CreditInfo c1 = new CreditInfo();
        c1.setHxqyName("湖南某某核心企业A");
        c1.setHxqyCode("91430100MA00000001");
        c1.setRetCode("00");
        c1.setRetMemo("查询成功");
        c1.setCreditInfoMx(List.of(bank1, bank2));

        // Entry 2: failure, RetMemo + CreditInfoMx both omitted (both optional)
        CreditInfo c2 = new CreditInfo();
        c2.setHxqyName("湖南某某核心企业B");
        c2.setHxqyCode("91430100MA00000002");
        c2.setRetCode("01");
        // RetMemo omitted (optional)
        // CreditInfoMx omitted (optional)

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3113回执扩展");

        HxqyCreditAmt3113 original = new HxqyCreditAmt3113();
        original.setSerialNo("SN3113-001");
        original.setSendNodeCode("A1000143000104");
        original.setDesNodeCode("B1001010203");
        original.setQueryDate("20260424");
        original.setCreditInfoNum("2");
        original.setCreditInfo(List.of(c1, c2));
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<hxqyCreditAmt3113")
                .contains("<SerialNo>SN3113-001</SerialNo>")
                .contains("<SendNodeCode>A1000143000104</SendNodeCode>")
                .contains("<DesNodeCode>B1001010203</DesNodeCode>")
                .contains("<QueryDate>20260424</QueryDate>")
                .contains("<CreditInfoNum>2</CreditInfoNum>")
                .contains("<CreditInfo>")
                .contains("<RetCode>00</RetCode>")
                .contains("<RetMemo>查询成功</RetMemo>")
                .contains("<CreditInfoMx>")
                .contains("<BankNodeCode>B1001010203</BankNodeCode>")
                .contains("<BankName>某某银行湖南分行</BankName>")
                .contains("<sxAmt>50000000.00</sxAmt>")
                .contains("<sxBalance>18000000.00</sxBalance>")
                .contains("<QueryReturnTime>20260424103500</QueryReturnTime>")
                .contains("<BankNodeCode>B1001020304</BankNodeCode>")
                .contains("<RetCode>01</RetCode>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3113回执扩展</ExtData>");

        HxqyCreditAmt3113 parsed = unmarshal(xml, HxqyCreditAmt3113.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3113-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getDesNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getQueryDate()).isEqualTo("20260424");
        assertThat(parsed.getCreditInfoNum()).isEqualTo("2");

        List<CreditInfo> cs = parsed.getCreditInfo();
        assertThat(cs).hasSize(2);

        CreditInfo p1 = cs.get(0);
        assertThat(p1.getHxqyName()).isEqualTo("湖南某某核心企业A");
        assertThat(p1.getHxqyCode()).isEqualTo("91430100MA00000001");
        assertThat(p1.getRetCode()).isEqualTo("00");
        assertThat(p1.getRetMemo()).isEqualTo("查询成功");

        List<CreditInfoBank> banks = p1.getCreditInfoMx();
        assertThat(banks).hasSize(2);
        assertThat(banks.get(0).getBankNodeCode()).isEqualTo("B1001010203");
        assertThat(banks.get(0).getBankName()).isEqualTo("某某银行湖南分行");
        assertThat(banks.get(0).getSxAmt()).isEqualTo("50000000.00");
        assertThat(banks.get(0).getSxBalance()).isEqualTo("18000000.00");
        assertThat(banks.get(0).getQueryReturnTime()).isEqualTo("20260424103500");
        assertThat(banks.get(1).getBankNodeCode()).isEqualTo("B1001020304");
        assertThat(banks.get(1).getBankName()).isEqualTo("另一家银行湖南分行");
        assertThat(banks.get(1).getSxAmt()).isEqualTo("30000000.00");
        assertThat(banks.get(1).getSxBalance()).isEqualTo("12500000.50");
        assertThat(banks.get(1).getQueryReturnTime()).isEqualTo("20260424103530");

        CreditInfo p2 = cs.get(1);
        assertThat(p2.getHxqyName()).isEqualTo("湖南某某核心企业B");
        assertThat(p2.getHxqyCode()).isEqualTo("91430100MA00000002");
        assertThat(p2.getRetCode()).isEqualTo("01");
        assertThat(p2.getRetMemo()).isNull();
        assertThat(p2.getCreditInfoMx()).isNull();

        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3113回执扩展");
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
