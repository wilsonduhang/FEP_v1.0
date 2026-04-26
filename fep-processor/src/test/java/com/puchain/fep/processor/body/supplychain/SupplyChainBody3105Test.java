package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.FileInfo;
import com.puchain.fep.processor.body.common.PzInfo;
import com.puchain.fep.processor.body.common.QyAccInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3105 {@link RzApplyInfo3105} supply
 * chain Body POJO and its 5 supply-chain support types
 * ({@link RzAmtInfo3105}, {@link ServiceChargeInfo}, {@link InvoInfo},
 * {@link ContractInfo}, {@link SignInfo}) plus 5 body.common nested types
 * ({@link QyAccInfo} ×3, {@link PzInfo} ×2, {@link FileInfo} list, {@link ExtInfo}).
 *
 * <p>Verifies all 28 main fields and their nested complex blocks roundtrip correctly,
 * ensuring JAXB binding for every nested complexType in 3105.xsd.</p>
 *
 * <p><b>Context isolation</b>: This test creates a {@link JAXBContext} that includes
 * all 6 supply-chain classes (main + 5 supports) plus the 4 body.common classes,
 * but does NOT include {@link RzAmtInfo3009} — both classes share
 * {@code @XmlRootElement(name="rzAmtInfo")} and must remain in separate contexts
 * to avoid binding collisions.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3105Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    RzApplyInfo3105.class,
                    RzAmtInfo3105.class, ServiceChargeInfo.class, InvoInfo.class,
                    ContractInfo.class, SignInfo.class,
                    QyAccInfo.class, PzInfo.class, FileInfo.class, ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    void rzApplyInfo3105_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(RzApplyInfo3105.class)).isTrue();
    }

    @Test
    void rzApplyInfo3105_jaxbRoundtrip_shouldPreserveAllFieldsWithFullNesting() throws Exception {
        RzApplyInfo3105 original = buildFullWithAllNested();

        String xml = marshal(original);
        // Spot-check key XML elements (main + each support + each common nested)
        assertThat(xml)
                .contains("<rzApplyInfo3105")
                .contains("<SerialNo>SN3105-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<BranchBankCode>BR-CS-001</BranchBankCode>")
                .contains("<ApplyMode>1</ApplyMode>")
                .contains("<PlatApplyNo>PLAT-APPLY-3105-2026042400001</PlatApplyNo>")
                .contains("<StdBizMode>11</StdBizMode>")
                .contains("<hxqyName>湖南某某核心企业</hxqyName>")
                .contains("<hxqyCode>91430100MA00000001</hxqyCode>")
                .contains("<rzpzNo>PZ-3105-2026042400001</rzpzNo>")
                .contains("<dbqyName>湖南某某担保公司</dbqyName>")
                .contains("<rzqyName>湖南某某融资企业</rzqyName>")
                .contains("<rzqyCode>91430100MA00000002</rzqyCode>")
                .contains("<rzqyAddr>长沙市岳麓区xx路1号</rzqyAddr>")
                .contains("<rzqyPlatNo>RZQY-PLAT-001</rzqyPlatNo>")
                // 3 QyAccInfo nested
                .contains("<rzqyAccInfo>")
                .contains("<hxqyInterestInfo>")
                .contains("<RepayAccInfo>")
                // RzAmtInfo3105 (9 fields)
                .contains("<rzAmtInfo>")
                .contains("<rzAmt>1000000.00</rzAmt>")
                .contains("<ApplyDate>20260424</ApplyDate>")
                .contains("<EndDate>20270424</EndDate>")
                .contains("<rzPurpose>采购原料</rzPurpose>")
                // SignInfo
                .contains("<SignInfo>")
                .contains("<SignElement>hxqyName|rzqyName|rzAmt|ApplyDate|EndDate</SignElement>")
                // ServiceChargeInfo
                .contains("<ServiceChargeInfo>")
                .contains("<SCAccNo>6228480000000099999</SCAccNo>")
                .contains("<SCAmt>1000.00</SCAmt>")
                // 2 PzInfo nested
                .contains("<pzInfo>")
                .contains("<zpzInfo>")
                // InvoInfo list (2 entries)
                .contains("<InvoInfo>")
                .contains("<InvoSerial>1</InvoSerial>")
                .contains("<InvoSerial>2</InvoSerial>")
                // ContractInfo list (2 entries)
                .contains("<ContractInfo>")
                .contains("<ContractAmt>500000.00</ContractAmt>")
                .contains("<jfqyName>甲方公司</jfqyName>")
                // AttachFileInfo list (2 entries)
                .contains("<AttachFileInfo>")
                .contains("<Filename>contract.pdf</Filename>")
                .contains("<Filename>invoice.pdf</Filename>")
                // ExtInfo
                .contains("<ExtInfo>")
                .contains("<ExtData>3105扩展数据</ExtData>");

        RzApplyInfo3105 parsed = unmarshal(xml, RzApplyInfo3105.class);

        // 16 scalar main fields
        assertThat(parsed.getSerialNo()).isEqualTo("SN3105-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getBranchBankCode()).isEqualTo("BR-CS-001");
        assertThat(parsed.getApplyMode()).isEqualTo("1");
        assertThat(parsed.getPlatApplyNo()).isEqualTo("PLAT-APPLY-3105-2026042400001");
        assertThat(parsed.getStdBizMode()).isEqualTo("11");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南某某核心企业");
        assertThat(parsed.getHxqyCode()).isEqualTo("91430100MA00000001");
        assertThat(parsed.getRzpzNo()).isEqualTo("PZ-3105-2026042400001");
        assertThat(parsed.getDbqyName()).isEqualTo("湖南某某担保公司");
        assertThat(parsed.getDbqyCode()).isEqualTo("91430100MA00000003");
        assertThat(parsed.getRzqyName()).isEqualTo("湖南某某融资企业");
        assertThat(parsed.getRzqyCode()).isEqualTo("91430100MA00000002");
        assertThat(parsed.getRzqyAddr()).isEqualTo("长沙市岳麓区xx路1号");
        assertThat(parsed.getRzqyPlatNo()).isEqualTo("RZQY-PLAT-001");

        // 3 × QyAccInfo
        QyAccInfo rzqyAcc = parsed.getRzqyAccInfo();
        assertThat(rzqyAcc).isNotNull();
        assertThat(rzqyAcc.getAccName()).isEqualTo("湖南某某融资企业");
        assertThat(rzqyAcc.getAccNumber()).isEqualTo("6228480000000001234");
        assertThat(rzqyAcc.getAccBankName()).isEqualTo("中国工商银行长沙分行");
        assertThat(rzqyAcc.getAccBankCode()).isEqualTo("ICBKCNBJHNL");

        QyAccInfo hxInterest = parsed.getHxqyInterestInfo();
        assertThat(hxInterest).isNotNull();
        assertThat(hxInterest.getAccNumber()).isEqualTo("6228480000000005678");

        QyAccInfo repayAcc = parsed.getRepayAccInfo();
        assertThat(repayAcc).isNotNull();
        assertThat(repayAcc.getAccNumber()).isEqualTo("6228480000000009012");

        // RzAmtInfo3105 (9 fields)
        RzAmtInfo3105 amt = parsed.getRzAmtInfo();
        assertThat(amt).isNotNull();
        assertThat(amt.getRzAmt()).isEqualTo("1000000.00");
        assertThat(amt.getRzRate()).isEqualTo("0.045000");
        assertThat(amt.getLxAmt()).isEqualTo("45000.00");
        assertThat(amt.getBankSCAmt()).isEqualTo("500.00");
        assertThat(amt.getApplyDate()).isEqualTo("20260424");
        assertThat(amt.getEndDate()).isEqualTo("20270424");
        assertThat(amt.getFxMode()).isEqualTo("2");
        assertThat(amt.getFkcnNo()).isEqualTo("FKCN-2026042400001");
        assertThat(amt.getRzPurpose()).isEqualTo("采购原料");

        // SignInfo (4 fields)
        SignInfo sign = parsed.getSignInfo();
        assertThat(sign).isNotNull();
        assertThat(sign.getSignElement()).isEqualTo("hxqyName|rzqyName|rzAmt|ApplyDate|EndDate");
        assertThat(sign.getHxqySign()).isEqualTo("HXQY-SIGN-BASE64");
        assertThat(sign.getRzqySign()).isEqualTo("RZQY-SIGN-BASE64");
        assertThat(sign.getPlatSign()).isEqualTo("PLAT-SIGN-BASE64");

        // ServiceChargeInfo (8 fields)
        ServiceChargeInfo svc = parsed.getServiceChargeInfo();
        assertThat(svc).isNotNull();
        assertThat(svc.getScAccNo()).isEqualTo("6228480000000099999");
        assertThat(svc.getScAccName()).isEqualTo("代收手续费户");
        assertThat(svc.getScAccBankName()).isEqualTo("中国建设银行长沙分行");
        assertThat(svc.getScAccBankCode()).isEqualTo("PCBCCNBJHNL");
        assertThat(svc.getScRate()).isEqualTo("0.001000");
        assertThat(svc.getScAmtMin()).isEqualTo("100.00");
        assertThat(svc.getScAmt()).isEqualTo("1000.00");
        assertThat(svc.getScMemo()).isEqualTo("代收手续费备注");

        // 2 × PzInfo
        assertThat(parsed.getPzInfo()).isNotNull();
        assertThat(parsed.getPzInfo().getPzNo()).isEqualTo("PZ-3105-2026042400001");
        assertThat(parsed.getZpzInfo()).isNotNull();
        assertThat(parsed.getZpzInfo().getPzNo()).isEqualTo("ZPZ-MASTER-001");

        // InvoInfo list (2 entries, 15 fields each)
        List<InvoInfo> invos = parsed.getInvoInfo();
        assertThat(invos).hasSize(2);
        InvoInfo invo1 = invos.get(0);
        assertThat(invo1.getInvoSerial()).isEqualTo("1");
        assertThat(invo1.getContractNo()).isEqualTo("CONTRACT-2026-001");
        assertThat(invo1.getInvoNo()).isEqualTo("INVO20260424001");
        assertThat(invo1.getInvoAmtTax()).isEqualTo("110000.00");
        assertThat(invo1.getInvoAmt()).isEqualTo("100000.00");
        assertThat(invo1.getInvoDate()).isEqualTo("20260420");
        assertThat(invo1.getXsfName()).isEqualTo("销售方A");
        InvoInfo invo2 = invos.get(1);
        assertThat(invo2.getInvoSerial()).isEqualTo("2");
        assertThat(invo2.getInvoNo()).isEqualTo("INVO20260424002");
        assertThat(invo2.getGhfName()).isEqualTo("购货方B");

        // ContractInfo list (2 entries, 10 fields each)
        List<ContractInfo> contracts = parsed.getContractInfo();
        assertThat(contracts).hasSize(2);
        ContractInfo c1 = contracts.get(0);
        assertThat(c1.getContractNo()).isEqualTo("CONTRACT-2026-001");
        assertThat(c1.getContractAmt()).isEqualTo("500000.00");
        assertThat(c1.getJfqyName()).isEqualTo("甲方公司");
        assertThat(c1.getYfqyName()).isEqualTo("乙方公司");
        assertThat(c1.getSxDate()).isEqualTo("20260101");
        assertThat(c1.getQzDate()).isEqualTo("20251220");
        assertThat(c1.getContractFilename()).isEqualTo("contract1.pdf");
        ContractInfo c2 = contracts.get(1);
        assertThat(c2.getContractNo()).isEqualTo("CONTRACT-2026-002");
        assertThat(c2.getContractAmt()).isEqualTo("500000.00");

        // AttachFileInfo list (2 entries)
        List<FileInfo> attachments = parsed.getAttachFileInfo();
        assertThat(attachments).hasSize(2);
        assertThat(attachments.get(0).getFilename()).isEqualTo("contract.pdf");
        assertThat(attachments.get(1).getFilename()).isEqualTo("invoice.pdf");

        // ExtInfo
        ExtInfo ext = parsed.getExtInfo();
        assertThat(ext).isNotNull();
        assertThat(ext.getExtData()).isEqualTo("3105扩展数据");
        assertThat(ext.getExtJsonFilename()).isEqualTo("ext3105.json");
    }

    /**
     * Build a fully-populated {@link RzApplyInfo3105} with all 28 fields including
     * 5 supply-chain support types and 5 body.common nested types.
     */
    private RzApplyInfo3105 buildFullWithAllNested() {
        // 3 × QyAccInfo
        QyAccInfo rzqyAcc = new QyAccInfo();
        rzqyAcc.setAccName("湖南某某融资企业");
        rzqyAcc.setAccNumber("6228480000000001234");
        rzqyAcc.setAccBankName("中国工商银行长沙分行");
        rzqyAcc.setAccBankCode("ICBKCNBJHNL");

        QyAccInfo hxInterest = new QyAccInfo();
        hxInterest.setAccName("湖南某某核心企业");
        hxInterest.setAccNumber("6228480000000005678");
        hxInterest.setAccBankName("中国工商银行长沙分行");
        hxInterest.setAccBankCode("ICBKCNBJHNL");

        QyAccInfo repayAcc = new QyAccInfo();
        repayAcc.setAccName("湖南某某融资企业");
        repayAcc.setAccNumber("6228480000000009012");
        repayAcc.setAccBankName("中国农业银行长沙分行");
        repayAcc.setAccBankCode("ABOCCNBJHNL");

        // RzAmtInfo3105 (9 fields)
        RzAmtInfo3105 amt = new RzAmtInfo3105();
        amt.setRzAmt("1000000.00");
        amt.setRzRate("0.045000");
        amt.setLxAmt("45000.00");
        amt.setBankSCAmt("500.00");
        amt.setApplyDate("20260424");
        amt.setEndDate("20270424");
        amt.setFxMode("2");
        amt.setFkcnNo("FKCN-2026042400001");
        amt.setRzPurpose("采购原料");

        // SignInfo (4 fields)
        SignInfo sign = new SignInfo();
        sign.setSignElement("hxqyName|rzqyName|rzAmt|ApplyDate|EndDate");
        sign.setHxqySign("HXQY-SIGN-BASE64");
        sign.setRzqySign("RZQY-SIGN-BASE64");
        sign.setPlatSign("PLAT-SIGN-BASE64");

        // ServiceChargeInfo (8 fields)
        ServiceChargeInfo svc = new ServiceChargeInfo();
        svc.setScAccNo("6228480000000099999");
        svc.setScAccName("代收手续费户");
        svc.setScAccBankName("中国建设银行长沙分行");
        svc.setScAccBankCode("PCBCCNBJHNL");
        svc.setScRate("0.001000");
        svc.setScAmtMin("100.00");
        svc.setScAmt("1000.00");
        svc.setScMemo("代收手续费备注");

        // 2 × PzInfo (only minimum required fields populated; PzInfo has 33 fields, full
        // coverage is verified in body.common PzInfo round-trip tests)
        PzInfo pz = new PzInfo();
        pz.setPlatShortName("某平台");
        pz.setPlatCode("PLAT-001");
        pz.setExternalPlat("0");
        pz.setHxqyName("湖南某某核心企业");
        pz.setHxqyCode("91430100MA00000001");
        pz.setPzNo("PZ-3105-2026042400001");
        pz.setPzClass("01");
        pz.setPzFunction("01");
        pz.setKlzrfName("出票方");
        pz.setKlzrfCode("91430100MA00000005");
        pz.setJsqyName("接收企业");
        pz.setJsqyCode("91430100MA00000002");
        pz.setJsqyPlatNo("RZQY-PLAT-001");
        pz.setPzAmt("1000000.00");
        pz.setPzStartDate("20260424");
        pz.setPzEndDate("20270424");
        pz.setPzState("01");
        pz.setPzrzState("01");
        pz.setPzFlowNum("1");

        PzInfo zpz = new PzInfo();
        zpz.setPlatShortName("某平台");
        zpz.setPlatCode("PLAT-001");
        zpz.setExternalPlat("0");
        zpz.setHxqyName("湖南某某核心企业");
        zpz.setHxqyCode("91430100MA00000001");
        zpz.setPzNo("ZPZ-MASTER-001");
        zpz.setPzClass("01");
        zpz.setPzFunction("01");
        zpz.setKlzrfName("出票方");
        zpz.setKlzrfCode("91430100MA00000005");
        zpz.setJsqyName("接收企业");
        zpz.setJsqyCode("91430100MA00000002");
        zpz.setJsqyPlatNo("RZQY-PLAT-001");
        zpz.setPzAmt("2000000.00");
        zpz.setPzStartDate("20260424");
        zpz.setPzEndDate("20270424");
        zpz.setPzState("01");
        zpz.setPzrzState("01");
        zpz.setPzFlowNum("1");

        // InvoInfo list (2 entries)
        InvoInfo invo1 = new InvoInfo();
        invo1.setInvoSerial("1");
        invo1.setContractNo("CONTRACT-2026-001");
        invo1.setInvoCode("INVO-CODE-001");
        invo1.setInvoNo("INVO20260424001");
        invo1.setCheckCode("ABC123");
        invo1.setInvoAmtTax("110000.00");
        invo1.setInvoAmt("100000.00");
        invo1.setInvoDate("20260420");
        invo1.setInvoAmtUsed("100000.00");
        invo1.setInvoFilename("invo1.pdf");
        invo1.setInvoCAFilename("invo1.cer");
        invo1.setXsfName("销售方A");
        invo1.setKpfName("开票方A");
        invo1.setGhfName("购货方A");
        invo1.setSpfName("收票方A");

        InvoInfo invo2 = new InvoInfo();
        invo2.setInvoSerial("2");
        invo2.setContractNo("CONTRACT-2026-002");
        invo2.setInvoNo("INVO20260424002");
        invo2.setInvoAmtTax("110000.00");
        invo2.setInvoAmt("100000.00");
        invo2.setInvoDate("20260421");
        invo2.setGhfName("购货方B");

        // ContractInfo list (2 entries)
        ContractInfo c1 = new ContractInfo();
        c1.setContractNo("CONTRACT-2026-001");
        c1.setContractAmt("500000.00");
        c1.setJfqyName("甲方公司");
        c1.setJfqyCode("91430100MA00000010");
        c1.setYfqyName("乙方公司");
        c1.setYfqyCode("91430100MA00000011");
        c1.setSxDate("20260101");
        c1.setQzDate("20251220");
        c1.setContractFilename("contract1.pdf");
        c1.setCertFilename("contract1.cer");

        ContractInfo c2 = new ContractInfo();
        c2.setContractNo("CONTRACT-2026-002");
        c2.setContractAmt("500000.00");
        c2.setJfqyName("甲方公司");
        c2.setYfqyName("乙方公司");
        c2.setSxDate("20260201");

        // AttachFileInfo list (2 entries)
        FileInfo file1 = new FileInfo();
        file1.setFileType("contract");
        file1.setFilename("contract.pdf");
        file1.setFileMemo("合同附件");

        FileInfo file2 = new FileInfo();
        file2.setFileType("invoice");
        file2.setFilename("invoice.pdf");
        file2.setFileMemo("发票附件");

        // ExtInfo
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3105扩展数据");
        ext.setExtJsonFilename("ext3105.json");

        // Main RzApplyInfo3105 (16 scalar + 12 nested = 28 fields)
        RzApplyInfo3105 original = new RzApplyInfo3105();
        original.setSerialNo("SN3105-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode("A1000143000104");
        original.setBranchBankCode("BR-CS-001");
        original.setApplyMode("1");
        original.setPlatApplyNo("PLAT-APPLY-3105-2026042400001");
        original.setStdBizMode("11");
        original.setHxqyName("湖南某某核心企业");
        original.setHxqyCode("91430100MA00000001");
        original.setRzpzNo("PZ-3105-2026042400001");
        original.setDbqyName("湖南某某担保公司");
        original.setDbqyCode("91430100MA00000003");
        original.setRzqyName("湖南某某融资企业");
        original.setRzqyCode("91430100MA00000002");
        original.setRzqyAddr("长沙市岳麓区xx路1号");
        original.setRzqyPlatNo("RZQY-PLAT-001");
        original.setRzqyAccInfo(rzqyAcc);
        original.setRzAmtInfo(amt);
        original.setSignInfo(sign);
        original.setServiceChargeInfo(svc);
        original.setHxqyInterestInfo(hxInterest);
        original.setRepayAccInfo(repayAcc);
        original.setPzInfo(pz);
        original.setZpzInfo(zpz);
        original.setInvoInfo(List.of(invo1, invo2));
        original.setContractInfo(List.of(c1, c2));
        original.setAttachFileInfo(List.of(file1, file2));
        original.setExtInfo(ext);

        return original;
    }

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
