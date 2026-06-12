package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.QyAccInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3009 {@link RzReturnInfo3009} supply
 * chain Body POJO and its support types {@link RzAmtInfo3009} and {@link DbInfo}.
 *
 * <p>Verifies all 11 main fields plus nested {@link RzAmtInfo3009} (12 fields,
 * with {@code LoanAccInfo} = {@link QyAccInfo}), {@link DbInfo} (6 fields) and
 * optional {@link ExtInfo}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3009Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    RzReturnInfo3009.class, RzAmtInfo3009.class, DbInfo.class,
                    QyAccInfo.class, ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    void rzReturnInfo3009_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(RzReturnInfo3009.class)).isTrue();
    }

    @Test
    void rzReturnInfo3009_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        // build LoanAccInfo (QyAccInfo)
        QyAccInfo loanAcc = new QyAccInfo();
        loanAcc.setAccName("湖南某某融资企业");
        loanAcc.setAccNumber("6228480000000001234");
        loanAcc.setAccBankName("中国工商银行长沙分行");
        loanAcc.setAccBankCode("ICBKCNBJHNL");

        // build RzAmtInfo3009 (12 fields, all populated incl. optional)
        RzAmtInfo3009 amtInfo = new RzAmtInfo3009();
        amtInfo.setBillNo("BILL-2026042400001");
        amtInfo.setRzAmt("1000000.00");
        amtInfo.setRzStartDate("20260424");
        amtInfo.setRzEndDate("20270424");
        amtInfo.setLxAmt("45000.00");
        amtInfo.setRzRate("0.045000");
        amtInfo.setBankSCAmt("500.00");
        amtInfo.setBankSCRate("0.000500");
        amtInfo.setTotalSCAmt("1500.00");
        amtInfo.setRzNetAmt("998500.00");
        amtInfo.setRepayMode("等额本息");
        amtInfo.setLoanAccInfo(loanAcc);

        // build DbInfo (6 fields, all populated incl. optional)
        DbInfo dbInfo = new DbInfo();
        dbInfo.setDbAmt("1000000.00");
        dbInfo.setDbFee("2000.00");
        dbInfo.setDbRate("0.002000");
        dbInfo.setDbqyName("湖南某某融资担保公司");
        dbInfo.setDbNodeCode("B2002020304");
        dbInfo.setDbContractNo("DB-CONTRACT-2026042400001");

        // build ExtInfo
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3009附加数据");
        ext.setExtJsonFilename("ext3009.json");

        // build main RzReturnInfo3009 (11 fields)
        RzReturnInfo3009 original = new RzReturnInfo3009();
        original.setSerialNo("SN3009-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setPlatApplyNo("PLAT-APPLY-2026042400001");
        original.setHxqyName("湖南某某核心企业");
        original.setRzpzNo("PZ-2026042400001");
        original.setRzPhaseCode("11");
        original.setRzPhaseInfo("已放款");
        original.setRzAmtInfo(amtInfo);
        original.setDbInfo(dbInfo);
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<rzReturnInfo3009")
                .contains("<SerialNo>SN3009-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNodeCode>")
                .contains("<PlatApplyNo>PLAT-APPLY-2026042400001</PlatApplyNo>")
                .contains("<hxqyName>湖南某某核心企业</hxqyName>")
                .contains("<rzpzNo>PZ-2026042400001</rzpzNo>")
                .contains("<rzPhaseCode>11</rzPhaseCode>")
                .contains("<rzPhaseInfo>已放款</rzPhaseInfo>")
                .contains("<rzAmtInfo>")
                .contains("<BillNo>BILL-2026042400001</BillNo>")
                .contains("<rzAmt>1000000.00</rzAmt>")
                .contains("<BankSCAmt>500.00</BankSCAmt>")
                .contains("<RepayMode>等额本息</RepayMode>")
                .contains("<LoanAccInfo>")
                .contains("<AccName>湖南某某融资企业</AccName>")
                .contains("<AccNumber>6228480000000001234</AccNumber>")
                .contains("<dbInfo>")
                .contains("<dbAmt>1000000.00</dbAmt>")
                .contains("<dbqyName>湖南某某融资担保公司</dbqyName>")
                .contains("<dbContractNo>DB-CONTRACT-2026042400001</dbContractNo>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3009附加数据</ExtData>");

        RzReturnInfo3009 parsed = unmarshal(xml, RzReturnInfo3009.class);
        // 11 main fields
        assertThat(parsed.getSerialNo()).isEqualTo("SN3009-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getPlatApplyNo()).isEqualTo("PLAT-APPLY-2026042400001");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南某某核心企业");
        assertThat(parsed.getRzpzNo()).isEqualTo("PZ-2026042400001");
        assertThat(parsed.getRzPhaseCode()).isEqualTo("11");
        assertThat(parsed.getRzPhaseInfo()).isEqualTo("已放款");
        assertThat(parsed.getRzAmtInfo()).isNotNull();
        assertThat(parsed.getDbInfo()).isNotNull();
        assertThat(parsed.getExtInfo()).isNotNull();

        // RzAmtInfo3009 12 fields
        RzAmtInfo3009 parsedAmt = parsed.getRzAmtInfo();
        assertThat(parsedAmt.getBillNo()).isEqualTo("BILL-2026042400001");
        assertThat(parsedAmt.getRzAmt()).isEqualTo("1000000.00");
        assertThat(parsedAmt.getRzStartDate()).isEqualTo("20260424");
        assertThat(parsedAmt.getRzEndDate()).isEqualTo("20270424");
        assertThat(parsedAmt.getLxAmt()).isEqualTo("45000.00");
        assertThat(parsedAmt.getRzRate()).isEqualTo("0.045000");
        assertThat(parsedAmt.getBankSCAmt()).isEqualTo("500.00");
        assertThat(parsedAmt.getBankSCRate()).isEqualTo("0.000500");
        assertThat(parsedAmt.getTotalSCAmt()).isEqualTo("1500.00");
        assertThat(parsedAmt.getRzNetAmt()).isEqualTo("998500.00");
        assertThat(parsedAmt.getRepayMode()).isEqualTo("等额本息");
        assertThat(parsedAmt.getLoanAccInfo()).isNotNull();
        assertThat(parsedAmt.getLoanAccInfo().getAccName()).isEqualTo("湖南某某融资企业");
        assertThat(parsedAmt.getLoanAccInfo().getAccNumber()).isEqualTo("6228480000000001234");
        assertThat(parsedAmt.getLoanAccInfo().getAccBankName()).isEqualTo("中国工商银行长沙分行");
        assertThat(parsedAmt.getLoanAccInfo().getAccBankCode()).isEqualTo("ICBKCNBJHNL");

        // DbInfo 6 fields
        DbInfo parsedDb = parsed.getDbInfo();
        assertThat(parsedDb.getDbAmt()).isEqualTo("1000000.00");
        assertThat(parsedDb.getDbFee()).isEqualTo("2000.00");
        assertThat(parsedDb.getDbRate()).isEqualTo("0.002000");
        assertThat(parsedDb.getDbqyName()).isEqualTo("湖南某某融资担保公司");
        assertThat(parsedDb.getDbNodeCode()).isEqualTo("B2002020304");
        assertThat(parsedDb.getDbContractNo()).isEqualTo("DB-CONTRACT-2026042400001");

        // ExtInfo
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3009附加数据");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3009.json");
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
