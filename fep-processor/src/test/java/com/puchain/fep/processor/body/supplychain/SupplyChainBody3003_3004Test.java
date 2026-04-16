package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.PzInfo;
import com.puchain.fep.processor.body.common.PzrzStatusInfo;
import com.puchain.fep.processor.body.common.RiskRate;
import com.puchain.fep.processor.body.common.ZpzAllInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3003 {@link PzInfoQuery3003}
 * and 3004 {@link PzInfoReturn3004} supply chain Body POJOs.
 *
 * <p>3004 is the most complex body type with nested {@link List} fields
 * ({@code List<RiskRate>} maxOccurs=10, {@code List<ZpzAllInfo>} maxOccurs=200).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3003_3004Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    PzInfoQuery3003.class, PzInfoReturn3004.class,
                    ExtInfo.class, PzrzStatusInfo.class, RiskRate.class,
                    PzInfo.class, ZpzAllInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── PzInfoQuery3003 ─────────────────────────────────────

    @Test
    void pzInfoQuery3003_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(PzInfoQuery3003.class)).isTrue();
    }

    @Test
    void pzInfoQuery3003_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        PzInfoQuery3003 original = new PzInfoQuery3003();
        original.setSerialNo("SN3003-001");
        original.setSendNodeCode("12345678901234");
        original.setDesNodeCode("A1000143000104");
        original.setHxqyName("湖南核心企业有限公司");
        original.setHxqyCode("123456789012345678");
        original.setPzNo("PZ20260416001");

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3003附加数据");
        ext.setExtJsonFilename("ext3003.json");
        original.setExtInfo(ext);

        String xml = marshal(original);

        assertThat(xml)
                .contains("<pzInfoQuery3003")
                .contains("<SerialNo>SN3003-001</SerialNo>")
                .contains("<SendNodeCode>12345678901234</SendNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<hxqyName>湖南核心企业有限公司</hxqyName>")
                .contains("<hxqyCode>123456789012345678</hxqyCode>")
                .contains("<pzNo>PZ20260416001</pzNo>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3003附加数据</ExtData>")
                .contains("<ExtJSONFilename>ext3003.json</ExtJSONFilename>");

        PzInfoQuery3003 parsed = unmarshal(xml, PzInfoQuery3003.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3003-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("12345678901234");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南核心企业有限公司");
        assertThat(parsed.getHxqyCode()).isEqualTo("123456789012345678");
        assertThat(parsed.getPzNo()).isEqualTo("PZ20260416001");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3003附加数据");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3003.json");
    }

    @Test
    void pzInfoQuery3003_optionalExtInfo_shouldBeOmittedWhenNull() throws Exception {
        PzInfoQuery3003 minimal = new PzInfoQuery3003();
        minimal.setSerialNo("SN3003-002");
        minimal.setSendNodeCode("12345678901234");
        minimal.setDesNodeCode("A1000143000104");
        minimal.setHxqyName("最小测试企业");
        minimal.setHxqyCode("123456789012345678");
        minimal.setPzNo("PZ20260416002");

        String xml = marshal(minimal);

        assertThat(xml).doesNotContain("<ExtInfo>");
    }

    // ── PzInfoReturn3004 ────────────────────────────────────

    @Test
    void pzInfoReturn3004_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(PzInfoReturn3004.class)).isTrue();
    }

    @Test
    void pzInfoReturn3004_fullRoundtrip_withNestedLists() throws Exception {
        PzInfoReturn3004 original = new PzInfoReturn3004();
        original.setSerialNo("SN3004-001");
        original.setSendNodeCode("12345678901234");
        original.setDesNodeCode("A1000143000104");
        original.setHxqyName("湖南核心企业有限公司");
        original.setHxqyCode("123456789012345678");
        original.setPzNo("PZ20260416001");
        original.setPzState("01");
        original.setPzrzState("02");

        // required nested pzrzStatusInfo
        PzrzStatusInfo statusInfo = new PzrzStatusInfo();
        statusInfo.setPzNo("PZ20260416001");
        statusInfo.setRzPhaseCode("03");
        statusInfo.setBankNodeCode("B1000143000101");
        original.setPzrzStatusInfo(statusInfo);

        // optional List<RiskRate> with 2 items
        RiskRate rate1 = new RiskRate();
        rate1.setRate("5.25");
        rate1.setRateMemo("基准利率");
        RiskRate rate2 = new RiskRate();
        rate2.setRate("6.50");
        rate2.setRateMemo("浮动利率");
        original.setRiskRateList(List.of(rate1, rate2));

        // optional edUpdateDateTime
        original.setEdUpdateDateTime("20260416120000");

        // optional pzInfo
        PzInfo pzDetail = new PzInfo();
        pzDetail.setPlatShortName("测试平台");
        pzDetail.setPlatCode("PLAT001");
        pzDetail.setExternalPlat("N");
        pzDetail.setHxqyName("湖南核心企业有限公司");
        pzDetail.setHxqyCode("123456789012345678");
        pzDetail.setPzNo("PZ20260416001");
        pzDetail.setPzClass("01");
        pzDetail.setPzFunction("融资");
        pzDetail.setKlzrfName("开立转让方");
        pzDetail.setKlzrfCode("KLZRF001");
        pzDetail.setJsqyName("接收企业");
        pzDetail.setJsqyCode("JSQY001");
        pzDetail.setJsqyPlatNo("JP001");
        pzDetail.setPzAmt("10000000.00");
        pzDetail.setPzStartDate("20260101");
        pzDetail.setPzEndDate("20261231");
        pzDetail.setPzState("01");
        pzDetail.setPzrzState("02");
        pzDetail.setPzFlowNum("5");
        original.setPzInfo(pzDetail);

        // optional List<ZpzAllInfo> with 2 items
        ZpzAllInfo zpz1 = new ZpzAllInfo();
        zpz1.setSerialNumber("ZPZ001");
        zpz1.setPzNo("PZ20260416001");
        zpz1.setPzClass("01");
        zpz1.setQyAssignName("转让方企业A");
        zpz1.setQyAssignCode("QA001");
        zpz1.setQyRecvName("接收方企业B");
        zpz1.setQyRecvCode("QR001");
        zpz1.setAmt("5000000.00");
        zpz1.setUpdateDate("20260416");
        zpz1.setPzFunction("融资");
        zpz1.setPzState("01");
        zpz1.setPzrzState("02");
        zpz1.setPzMajorNo("MAJ001");
        zpz1.setLoanAmt("4000000.00");
        zpz1.setSubState("00");

        ZpzAllInfo zpz2 = new ZpzAllInfo();
        zpz2.setSerialNumber("ZPZ002");
        zpz2.setPzNo("PZ20260416001");
        zpz2.setPzClass("02");
        zpz2.setQyAssignName("转让方企业C");
        zpz2.setQyAssignCode("QA002");
        zpz2.setQyRecvName("接收方企业D");
        zpz2.setQyRecvCode("QR002");
        zpz2.setAmt("3000000.00");
        zpz2.setUpdateDate("20260416");
        zpz2.setPzFunction("融资");
        zpz2.setPzState("02");
        zpz2.setPzrzState("03");
        zpz2.setPzMajorNo("MAJ002");
        zpz2.setLoanAmt("2500000.00");
        zpz2.setSubState("01");
        original.setZpzAllInfoList(List.of(zpz1, zpz2));

        // optional ExtInfo
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3004完整测试扩展");
        original.setExtInfo(ext);

        // Marshal
        String xml = marshal(original);

        // Verify XML structure
        assertThat(xml)
                .contains("<pzInfoReturn3004")
                .contains("<SerialNo>SN3004-001</SerialNo>")
                .contains("<SendNodeCode>12345678901234</SendNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<hxqyName>湖南核心企业有限公司</hxqyName>")
                .contains("<hxqyCode>123456789012345678</hxqyCode>")
                .contains("<pzNo>PZ20260416001</pzNo>")
                .contains("<pzState>01</pzState>")
                .contains("<pzrzState>02</pzrzState>")
                // pzrzStatusInfo nested
                .contains("<pzrzStatusInfo>")
                .contains("<rzPhaseCode>03</rzPhaseCode>")
                .contains("<BankNodeCode>B1000143000101</BankNodeCode>")
                // RiskRate repeated elements
                .contains("<RiskRate>")
                .contains("<Rate>5.25</Rate>")
                .contains("<RateMemo>基准利率</RateMemo>")
                .contains("<Rate>6.50</Rate>")
                .contains("<RateMemo>浮动利率</RateMemo>")
                // edUpdateDateTime
                .contains("<edUpdateDateTime>20260416120000</edUpdateDateTime>")
                // pzInfo nested
                .contains("<pzInfo>")
                .contains("<PlatShortName>测试平台</PlatShortName>")
                // zpzAllInfo repeated elements
                .contains("<zpzAllInfo>")
                .contains("<SerialNumber>ZPZ001</SerialNumber>")
                .contains("<SerialNumber>ZPZ002</SerialNumber>")
                // ExtInfo
                .contains("<ExtInfo>")
                .contains("<ExtData>3004完整测试扩展</ExtData>");

        // Unmarshal and verify
        PzInfoReturn3004 parsed = unmarshal(xml, PzInfoReturn3004.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3004-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("12345678901234");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南核心企业有限公司");
        assertThat(parsed.getHxqyCode()).isEqualTo("123456789012345678");
        assertThat(parsed.getPzNo()).isEqualTo("PZ20260416001");
        assertThat(parsed.getPzState()).isEqualTo("01");
        assertThat(parsed.getPzrzState()).isEqualTo("02");

        // pzrzStatusInfo
        assertThat(parsed.getPzrzStatusInfo()).isNotNull();
        assertThat(parsed.getPzrzStatusInfo().getPzNo()).isEqualTo("PZ20260416001");
        assertThat(parsed.getPzrzStatusInfo().getRzPhaseCode()).isEqualTo("03");
        assertThat(parsed.getPzrzStatusInfo().getBankNodeCode()).isEqualTo("B1000143000101");

        // List<RiskRate>
        assertThat(parsed.getRiskRateList()).hasSize(2);
        assertThat(parsed.getRiskRateList().get(0).getRate()).isEqualTo("5.25");
        assertThat(parsed.getRiskRateList().get(0).getRateMemo()).isEqualTo("基准利率");
        assertThat(parsed.getRiskRateList().get(1).getRate()).isEqualTo("6.50");
        assertThat(parsed.getRiskRateList().get(1).getRateMemo()).isEqualTo("浮动利率");

        // edUpdateDateTime
        assertThat(parsed.getEdUpdateDateTime()).isEqualTo("20260416120000");

        // pzInfo
        assertThat(parsed.getPzInfo()).isNotNull();
        assertThat(parsed.getPzInfo().getPlatShortName()).isEqualTo("测试平台");
        assertThat(parsed.getPzInfo().getPlatCode()).isEqualTo("PLAT001");

        // List<ZpzAllInfo>
        assertThat(parsed.getZpzAllInfoList()).hasSize(2);
        assertThat(parsed.getZpzAllInfoList().get(0).getSerialNumber()).isEqualTo("ZPZ001");
        assertThat(parsed.getZpzAllInfoList().get(0).getAmt()).isEqualTo("5000000.00");
        assertThat(parsed.getZpzAllInfoList().get(1).getSerialNumber()).isEqualTo("ZPZ002");
        assertThat(parsed.getZpzAllInfoList().get(1).getAmt()).isEqualTo("3000000.00");

        // ExtInfo
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3004完整测试扩展");
    }

    @Test
    void pzInfoReturn3004_minimal_onlyRequiredFields() throws Exception {
        PzInfoReturn3004 minimal = new PzInfoReturn3004();
        minimal.setSerialNo("SN3004-MIN");
        minimal.setSendNodeCode("12345678901234");
        minimal.setDesNodeCode("A1000143000104");
        minimal.setHxqyName("最小测试企业");
        minimal.setHxqyCode("123456789012345678");
        minimal.setPzNo("PZ20260416MIN");
        minimal.setPzState("99");
        minimal.setPzrzState("00");

        PzrzStatusInfo statusInfo = new PzrzStatusInfo();
        statusInfo.setPzNo("PZ20260416MIN");
        statusInfo.setRzPhaseCode("01");
        statusInfo.setBankNodeCode("B1000143000199");
        minimal.setPzrzStatusInfo(statusInfo);

        String xml = marshal(minimal);

        // Optional fields should be absent
        assertThat(xml)
                .doesNotContain("<RiskRate>")
                .doesNotContain("<edUpdateDateTime>")
                .doesNotContain("<pzInfo>")
                .doesNotContain("<zpzAllInfo>")
                .doesNotContain("<ExtInfo>");

        // Required fields present
        assertThat(xml)
                .contains("<pzInfoReturn3004")
                .contains("<SerialNo>SN3004-MIN</SerialNo>")
                .contains("<pzrzStatusInfo>");

        // Roundtrip
        PzInfoReturn3004 parsed = unmarshal(xml, PzInfoReturn3004.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3004-MIN");
        assertThat(parsed.getPzrzStatusInfo()).isNotNull();
        assertThat(parsed.getRiskRateList()).isNull();
        assertThat(parsed.getPzInfo()).isNull();
        assertThat(parsed.getZpzAllInfoList()).isNull();
        assertThat(parsed.getExtInfo()).isNull();
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
