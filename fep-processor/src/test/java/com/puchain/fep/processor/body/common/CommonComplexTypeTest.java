package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for shared complexType POJOs
 * (ExtInfo, PzrzStatusInfo, RiskRate, ZpzAllInfo, PzInfo).
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CommonComplexTypeTest {

    // ── ExtInfo ────────────────────────────────────────────

    @Test
    void extInfo_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(ExtInfo.class)).isTrue();
    }

    @Test
    void extInfo_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        ExtInfo original = new ExtInfo();
        original.setExtJsonFilename("data.json");
        original.setExtData("sample ext data value");
        original.setExtGeneralFilename("general.dat");
        original.setExtReserve1("reserve1");
        original.setExtReserve2("reserve2");
        original.setExtReserve3("reserve3");
        original.setExtReserve4("reserve4");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<extInfo")
                .contains("<ExtJSONFilename>data.json</ExtJSONFilename>")
                .contains("<ExtData>sample ext data value</ExtData>")
                .contains("<ExtGeneralFilename>general.dat</ExtGeneralFilename>")
                .contains("<ExtReserve1>reserve1</ExtReserve1>")
                .contains("<ExtReserve2>reserve2</ExtReserve2>")
                .contains("<ExtReserve3>reserve3</ExtReserve3>")
                .contains("<ExtReserve4>reserve4</ExtReserve4>");

        ExtInfo parsed = JaxbRoundtripSupport.unmarshal(xml, ExtInfo.class);
        assertThat(parsed.getExtJsonFilename()).isEqualTo("data.json");
        assertThat(parsed.getExtData()).isEqualTo("sample ext data value");
        assertThat(parsed.getExtGeneralFilename()).isEqualTo("general.dat");
        assertThat(parsed.getExtReserve1()).isEqualTo("reserve1");
        assertThat(parsed.getExtReserve2()).isEqualTo("reserve2");
        assertThat(parsed.getExtReserve3()).isEqualTo("reserve3");
        assertThat(parsed.getExtReserve4()).isEqualTo("reserve4");
    }

    @Test
    void extInfo_optionalFields_shouldBeOmittedWhenNull() throws Exception {
        ExtInfo empty = new ExtInfo();

        String xml = JaxbRoundtripSupport.marshal(empty);

        assertThat(xml)
                .doesNotContain("<ExtJSONFilename>")
                .doesNotContain("<ExtData>")
                .doesNotContain("<ExtGeneralFilename>")
                .doesNotContain("<ExtReserve1>")
                .doesNotContain("<ExtReserve2>")
                .doesNotContain("<ExtReserve3>")
                .doesNotContain("<ExtReserve4>");
    }

    // ── PzrzStatusInfo ─────────────────────────────────────

    @Test
    void pzrzStatusInfo_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(PzrzStatusInfo.class)).isTrue();
    }

    @Test
    void pzrzStatusInfo_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        PzrzStatusInfo original = new PzrzStatusInfo();
        original.setPzNo("PZ20260416000001");
        original.setRzPhaseCode("01");
        original.setBankNodeCode("12345678901234");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<pzrzStatusInfo")
                .contains("<pzNo>PZ20260416000001</pzNo>")
                .contains("<rzPhaseCode>01</rzPhaseCode>")
                .contains("<BankNodeCode>12345678901234</BankNodeCode>");

        PzrzStatusInfo parsed = JaxbRoundtripSupport.unmarshal(xml, PzrzStatusInfo.class);
        assertThat(parsed.getPzNo()).isEqualTo("PZ20260416000001");
        assertThat(parsed.getRzPhaseCode()).isEqualTo("01");
        assertThat(parsed.getBankNodeCode()).isEqualTo("12345678901234");
    }

    // ── RiskRate ───────────────────────────────────────────

    @Test
    void riskRate_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(RiskRate.class)).isTrue();
    }

    @Test
    void riskRate_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        RiskRate original = new RiskRate();
        original.setRate("5.25");
        original.setRateMemo("风险利率备注");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<RiskRate")
                .contains("<Rate>5.25</Rate>")
                .contains("<RateMemo>风险利率备注</RateMemo>");

        RiskRate parsed = JaxbRoundtripSupport.unmarshal(xml, RiskRate.class);
        assertThat(parsed.getRate()).isEqualTo("5.25");
        assertThat(parsed.getRateMemo()).isEqualTo("风险利率备注");
    }

    @Test
    void riskRate_optionalRateMemo_shouldBeOmittedWhenNull() throws Exception {
        RiskRate minimal = new RiskRate();
        minimal.setRate("3.50");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .contains("<Rate>3.50</Rate>")
                .doesNotContain("<RateMemo>");
    }

    // ── ZpzAllInfo ─────────────────────────────────────────

    @Test
    void zpzAllInfo_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(ZpzAllInfo.class)).isTrue();
    }

    @Test
    void zpzAllInfo_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        ZpzAllInfo original = new ZpzAllInfo();
        original.setSerialNumber("1");
        original.setPzNo("PZ20260416000002");
        original.setPzClass("01");
        original.setPreNo("PZ20260416000000");
        original.setQyAssignName("出让企业");
        original.setQyAssignCode("912345678901234567");
        original.setQyRecvName("接收企业");
        original.setQyRecvCode("918765432109876543");
        original.setAmt("1000000.00");
        original.setUpdateDate("20260416");
        original.setPzFunction("001");
        original.setPzState("01");
        original.setPzrzState("02");
        original.setPzMajorNo("PZ20260416000100");
        original.setLoanAmt("500000.00");
        original.setSubState("01");
        original.setReserve("备注信息");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<zpzAllInfo")
                .contains("<SerialNumber>1</SerialNumber>")
                .contains("<pzNo>PZ20260416000002</pzNo>")
                .contains("<pzClass>01</pzClass>")
                .contains("<PreNo>PZ20260416000000</PreNo>")
                .contains("<qyAssignName>出让企业</qyAssignName>")
                .contains("<qyAssignCode>912345678901234567</qyAssignCode>")
                .contains("<qyRecvName>接收企业</qyRecvName>")
                .contains("<qyRecvCode>918765432109876543</qyRecvCode>")
                .contains("<Amt>1000000.00</Amt>")
                .contains("<UpdateDate>20260416</UpdateDate>")
                .contains("<pzFunction>001</pzFunction>")
                .contains("<pzState>01</pzState>")
                .contains("<pzrzState>02</pzrzState>")
                .contains("<pzMajorNo>PZ20260416000100</pzMajorNo>")
                .contains("<LoanAmt>500000.00</LoanAmt>")
                .contains("<SubState>01</SubState>")
                .contains("<Reserve>备注信息</Reserve>");

        ZpzAllInfo parsed = JaxbRoundtripSupport.unmarshal(xml, ZpzAllInfo.class);
        assertThat(parsed.getSerialNumber()).isEqualTo("1");
        assertThat(parsed.getPzNo()).isEqualTo("PZ20260416000002");
        assertThat(parsed.getPzClass()).isEqualTo("01");
        assertThat(parsed.getPreNo()).isEqualTo("PZ20260416000000");
        assertThat(parsed.getQyAssignName()).isEqualTo("出让企业");
        assertThat(parsed.getQyAssignCode()).isEqualTo("912345678901234567");
        assertThat(parsed.getQyRecvName()).isEqualTo("接收企业");
        assertThat(parsed.getQyRecvCode()).isEqualTo("918765432109876543");
        assertThat(parsed.getAmt()).isEqualTo("1000000.00");
        assertThat(parsed.getUpdateDate()).isEqualTo("20260416");
        assertThat(parsed.getPzFunction()).isEqualTo("001");
        assertThat(parsed.getPzState()).isEqualTo("01");
        assertThat(parsed.getPzrzState()).isEqualTo("02");
        assertThat(parsed.getPzMajorNo()).isEqualTo("PZ20260416000100");
        assertThat(parsed.getLoanAmt()).isEqualTo("500000.00");
        assertThat(parsed.getSubState()).isEqualTo("01");
        assertThat(parsed.getReserve()).isEqualTo("备注信息");
    }

    @Test
    void zpzAllInfo_optionalFields_shouldBeOmittedWhenNull() throws Exception {
        ZpzAllInfo minimal = new ZpzAllInfo();
        minimal.setSerialNumber("1");
        minimal.setPzNo("PZ20260416000002");
        minimal.setPzClass("01");
        minimal.setQyAssignName("出让企业");
        minimal.setQyAssignCode("912345678901234567");
        minimal.setQyRecvName("接收企业");
        minimal.setQyRecvCode("918765432109876543");
        minimal.setAmt("1000000.00");
        minimal.setUpdateDate("20260416");
        minimal.setPzFunction("001");
        minimal.setPzState("01");
        minimal.setPzrzState("02");
        minimal.setPzMajorNo("PZ20260416000100");
        minimal.setLoanAmt("500000.00");
        minimal.setSubState("01");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .doesNotContain("<PreNo>")
                .doesNotContain("<Reserve>");
    }

    // ── PzInfo ─────────────────────────────────────────────

    @Test
    void pzInfo_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(PzInfo.class)).isTrue();
    }

    @Test
    void pzInfo_jaxbRoundtrip_shouldPreserveRequiredFields() throws Exception {
        PzInfo original = new PzInfo();
        original.setPlatShortName("湖南平台");
        original.setPlatCode("PLAT001");
        original.setExternalPlat("EXT001");
        original.setHxqyName("核心企业");
        original.setHxqyCode("912345678901234567");
        original.setPzNo("PZ20260416000003");
        original.setPzClass("01");
        original.setPzFunction("001");
        original.setKlzrfName("开立转让方");
        original.setKlzrfCode("918765432109876543");
        original.setJsqyName("接收企业");
        original.setJsqyCode("912345678901234568");
        original.setJsqyPlatNo("JSPLAT001");
        original.setPzAmt("2000000.00");
        original.setPzStartDate("20260416");
        original.setPzEndDate("20261016");
        original.setPzState("01");
        original.setPzrzState("02");
        original.setPzFlowNum("5");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<pzInfo")
                .contains("<PlatShortName>湖南平台</PlatShortName>")
                .contains("<PlatCode>PLAT001</PlatCode>")
                .contains("<ExternalPlat>EXT001</ExternalPlat>")
                .contains("<hxqyName>核心企业</hxqyName>")
                .contains("<hxqyCode>912345678901234567</hxqyCode>")
                .contains("<pzNo>PZ20260416000003</pzNo>")
                .contains("<pzClass>01</pzClass>")
                .contains("<pzFunction>001</pzFunction>")
                .contains("<klzrfName>开立转让方</klzrfName>")
                .contains("<klzrfCode>918765432109876543</klzrfCode>")
                .contains("<jsqyName>接收企业</jsqyName>")
                .contains("<jsqyCode>912345678901234568</jsqyCode>")
                .contains("<jsqyPlatNo>JSPLAT001</jsqyPlatNo>")
                .contains("<pzAmt>2000000.00</pzAmt>")
                .contains("<pzStartDate>20260416</pzStartDate>")
                .contains("<pzEndDate>20261016</pzEndDate>")
                .contains("<pzState>01</pzState>")
                .contains("<pzrzState>02</pzrzState>")
                .contains("<pzFlowNum>5</pzFlowNum>");

        PzInfo parsed = JaxbRoundtripSupport.unmarshal(xml, PzInfo.class);
        assertThat(parsed.getPlatShortName()).isEqualTo("湖南平台");
        assertThat(parsed.getPlatCode()).isEqualTo("PLAT001");
        assertThat(parsed.getExternalPlat()).isEqualTo("EXT001");
        assertThat(parsed.getHxqyName()).isEqualTo("核心企业");
        assertThat(parsed.getHxqyCode()).isEqualTo("912345678901234567");
        assertThat(parsed.getPzNo()).isEqualTo("PZ20260416000003");
        assertThat(parsed.getPzClass()).isEqualTo("01");
        assertThat(parsed.getPzFunction()).isEqualTo("001");
        assertThat(parsed.getKlzrfName()).isEqualTo("开立转让方");
        assertThat(parsed.getKlzrfCode()).isEqualTo("918765432109876543");
        assertThat(parsed.getJsqyName()).isEqualTo("接收企业");
        assertThat(parsed.getJsqyCode()).isEqualTo("912345678901234568");
        assertThat(parsed.getJsqyPlatNo()).isEqualTo("JSPLAT001");
        assertThat(parsed.getPzAmt()).isEqualTo("2000000.00");
        assertThat(parsed.getPzStartDate()).isEqualTo("20260416");
        assertThat(parsed.getPzEndDate()).isEqualTo("20261016");
        assertThat(parsed.getPzState()).isEqualTo("01");
        assertThat(parsed.getPzrzState()).isEqualTo("02");
        assertThat(parsed.getPzFlowNum()).isEqualTo("5");
    }

    @Test
    void pzInfo_jaxbRoundtrip_shouldPreserveOptionalFields() throws Exception {
        PzInfo original = new PzInfo();
        // set required fields
        original.setPlatShortName("湖南平台");
        original.setPlatCode("PLAT001");
        original.setExternalPlat("EXT001");
        original.setHxqyName("核心企业");
        original.setHxqyCode("912345678901234567");
        original.setPzNo("PZ20260416000003");
        original.setPzClass("01");
        original.setPzFunction("001");
        original.setKlzrfName("开立转让方");
        original.setKlzrfCode("918765432109876543");
        original.setJsqyName("接收企业");
        original.setJsqyCode("912345678901234568");
        original.setJsqyPlatNo("JSPLAT001");
        original.setPzAmt("2000000.00");
        original.setPzStartDate("20260416");
        original.setPzEndDate("20261016");
        original.setPzState("01");
        original.setPzrzState("02");
        original.setPzFlowNum("5");
        // set optional fields
        original.setPzPreNo("PZ20260416000000");
        original.setPzMajorNo("PZ20260416000100");
        original.setPzrzSubAmt("100000.00");
        original.setPzFilename("pz_file.pdf");
        original.setSignElement("SIGN_ELEM");
        original.setKlzrfSign("KLZRF_SIGN_DATA");
        original.setPlatSign("PLAT_SIGN_DATA");
        original.setRemainQuota("900000.00");
        original.setFxftRatio("0.50");
        original.setFfftRatio("0.30");
        original.setFkcnNo("FKCN001");
        original.setFkcnFile("fkcn.pdf");
        original.setPzMemo("凭证备注信息");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<pzPreNo>PZ20260416000000</pzPreNo>")
                .contains("<pzMajorNo>PZ20260416000100</pzMajorNo>")
                .contains("<pzrzSubAmt>100000.00</pzrzSubAmt>")
                .contains("<pzFilename>pz_file.pdf</pzFilename>")
                .contains("<SignElement>SIGN_ELEM</SignElement>")
                .contains("<klzrfSign>KLZRF_SIGN_DATA</klzrfSign>")
                .contains("<PlatSign>PLAT_SIGN_DATA</PlatSign>")
                .contains("<RemainQuota>900000.00</RemainQuota>")
                .contains("<fxftRatio>0.50</fxftRatio>")
                .contains("<ffftRatio>0.30</ffftRatio>")
                .contains("<fkcnNo>FKCN001</fkcnNo>")
                .contains("<fkcnFile>fkcn.pdf</fkcnFile>")
                .contains("<pzMemo>凭证备注信息</pzMemo>");

        PzInfo parsed = JaxbRoundtripSupport.unmarshal(xml, PzInfo.class);
        assertThat(parsed.getPzPreNo()).isEqualTo("PZ20260416000000");
        assertThat(parsed.getPzMajorNo()).isEqualTo("PZ20260416000100");
        assertThat(parsed.getPzrzSubAmt()).isEqualTo("100000.00");
        assertThat(parsed.getPzFilename()).isEqualTo("pz_file.pdf");
        assertThat(parsed.getSignElement()).isEqualTo("SIGN_ELEM");
        assertThat(parsed.getKlzrfSign()).isEqualTo("KLZRF_SIGN_DATA");
        assertThat(parsed.getPlatSign()).isEqualTo("PLAT_SIGN_DATA");
        assertThat(parsed.getRemainQuota()).isEqualTo("900000.00");
        assertThat(parsed.getFxftRatio()).isEqualTo("0.50");
        assertThat(parsed.getFfftRatio()).isEqualTo("0.30");
        assertThat(parsed.getFkcnNo()).isEqualTo("FKCN001");
        assertThat(parsed.getFkcnFile()).isEqualTo("fkcn.pdf");
        assertThat(parsed.getPzMemo()).isEqualTo("凭证备注信息");
    }

    @Test
    void pzInfo_optionalFields_shouldBeOmittedWhenNull() throws Exception {
        PzInfo minimal = new PzInfo();
        minimal.setPlatShortName("湖南平台");
        minimal.setPlatCode("PLAT001");
        minimal.setExternalPlat("EXT001");
        minimal.setHxqyName("核心企业");
        minimal.setHxqyCode("912345678901234567");
        minimal.setPzNo("PZ20260416000003");
        minimal.setPzClass("01");
        minimal.setPzFunction("001");
        minimal.setKlzrfName("开立转让方");
        minimal.setKlzrfCode("918765432109876543");
        minimal.setJsqyName("接收企业");
        minimal.setJsqyCode("912345678901234568");
        minimal.setJsqyPlatNo("JSPLAT001");
        minimal.setPzAmt("2000000.00");
        minimal.setPzStartDate("20260416");
        minimal.setPzEndDate("20261016");
        minimal.setPzState("01");
        minimal.setPzrzState("02");
        minimal.setPzFlowNum("5");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .doesNotContain("<pzPreNo>")
                .doesNotContain("<pzMajorNo>")
                .doesNotContain("<pzrzSubAmt>")
                .doesNotContain("<pzFilename>")
                .doesNotContain("<SignElement>")
                .doesNotContain("<klzrfSign>")
                .doesNotContain("<PlatSign>")
                .doesNotContain("<RemainQuota>")
                .doesNotContain("<fxftRatio>")
                .doesNotContain("<ffftRatio>")
                .doesNotContain("<fkcnNo>")
                .doesNotContain("<fkcnFile>")
                .doesNotContain("<pzMemo>");
    }
}
