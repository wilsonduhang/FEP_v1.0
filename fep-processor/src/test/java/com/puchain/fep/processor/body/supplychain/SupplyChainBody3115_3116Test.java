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
 * JAXB marshal/unmarshal roundtrip tests for 3115 {@link PlatPay3115} and
 * 3116 {@link BankCheckDay3116} supply chain Body POJOs plus their supplychain
 * support types ({@link QsInfo}, {@link QsReturnInfo}, {@link CheckDetailInfo}).
 *
 * <p>Verifies all 11 main fields of {@link PlatPay3115} (including the nested
 * {@code List<QsInfo>} and a sample {@link QsReturnInfo} bank-receipt block),
 * and all 9 main fields of {@link BankCheckDay3116} (including the nested
 * {@code List<CheckDetailInfo>} of 17 fields each, both with all-required
 * filled and with optional fields omitted).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3115_3116Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    PlatPay3115.class, BankCheckDay3116.class,
                    QsInfo.class, QsReturnInfo.class, CheckDetailInfo.class,
                    ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── PlatPay3115 ───────────────────────────────────────────

    @Test
    void platPay3115_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(PlatPay3115.class)).isTrue();
    }

    @Test
    void platPay3115_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        // Settlement instruction #1 — payment with bank receipt
        QsReturnInfo ret1 = new QsReturnInfo();
        ret1.setQsReturnBankName("某某银行湖南分行");
        ret1.setQsReturnCode("00");
        ret1.setQsReturnSerialNo("BANKSN3115-001");
        ret1.setQsReturnDate("20260424");
        ret1.setQsReturnFilename("receipt-3115-001.pdf");
        ret1.setQsReturnMemo("清算成功");

        QsInfo qs1 = new QsInfo();
        qs1.setQsSerialNo("QSSN3115-001");
        qs1.setPzNo("PZ3115001");
        qs1.setFkfAccName("付款方公司A");
        qs1.setFkfAccNo("6225880100000001");
        qs1.setSkfAccName("收款方公司A");
        qs1.setSkfAccNo("6225880100000002");
        qs1.setSkfBankCode("102100099996");
        qs1.setAmt("1000000.00");
        qs1.setWishDate("20260425");
        qs1.setQsPostscript("供应链清算款A");
        qs1.setQsMemo("月度结算");
        qs1.setQsReturnInfo(ret1);

        // Settlement instruction #2 — pending (no bank receipt yet, optional fields omitted)
        QsInfo qs2 = new QsInfo();
        qs2.setQsSerialNo("QSSN3115-002");
        qs2.setFkfAccName("付款方公司B");
        qs2.setFkfAccNo("6225880100000003");
        qs2.setSkfAccName("收款方公司B");
        qs2.setSkfAccNo("6225880100000004");
        qs2.setAmt("500000.00");
        qs2.setWishDate("20260425");
        // pzNo / skfBankCode / qsPostscript / qsMemo / qsReturnInfo all omitted

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3115附加数据");

        PlatPay3115 original = new PlatPay3115();
        original.setSerialNo("SN3115-MAIN-001");
        original.setSendNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setDesNodeCode("B1001010203");
        original.setPlatPayNo("PLATPAY3115001");
        original.setHxqyName("湖南某某核心企业A");
        original.setHxqyCode("91430100MA00000001");
        original.setQsInfo(List.of(qs1, qs2));
        original.setSignElement("fkrAccName|fkrAccNo|skrAccName|skrAccNo|Amt|WishDate");
        original.setQsfqSign("BASE64ENCODEDPK7SIGN==");
        original.setPlatSign("BASE64ENCODEDPK7PLATSIGN==");
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<PlatPay3115")
                .contains("<SerialNo>SN3115-MAIN-001</SerialNo>")
                .contains("<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>")
                .contains("<DesNodeCode>B1001010203</DesNodeCode>")
                .contains("<PlatPayNo>PLATPAY3115001</PlatPayNo>")
                .contains("<hxqyName>湖南某某核心企业A</hxqyName>")
                .contains("<hxqyCode>91430100MA00000001</hxqyCode>")
                .contains("<qsInfo>")
                .contains("<qsSerialNo>QSSN3115-001</qsSerialNo>")
                .contains("<pzNo>PZ3115001</pzNo>")
                .contains("<fkfAccName>付款方公司A</fkfAccName>")
                .contains("<skfAccNo>6225880100000002</skfAccNo>")
                .contains("<skfBankCode>102100099996</skfBankCode>")
                .contains("<Amt>1000000.00</Amt>")
                .contains("<WishDate>20260425</WishDate>")
                .contains("<qsReturnInfo>")
                .contains("<qsReturnBankName>某某银行湖南分行</qsReturnBankName>")
                .contains("<qsReturnCode>00</qsReturnCode>")
                .contains("<qsReturnSerialNo>BANKSN3115-001</qsReturnSerialNo>")
                .contains("<qsReturnFilename>receipt-3115-001.pdf</qsReturnFilename>")
                .contains("<SignElement>fkrAccName|fkrAccNo|skrAccName|skrAccNo|Amt|WishDate</SignElement>")
                .contains("<qsfqSign>BASE64ENCODEDPK7SIGN==</qsfqSign>")
                .contains("<PlatSign>BASE64ENCODEDPK7PLATSIGN==</PlatSign>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3115附加数据</ExtData>");

        // Field order check (XSD propOrder: SerialNo < PlatPayNo < qsInfo < SignElement < ExtInfo)
        int idxSerial = xml.indexOf("<SerialNo>");
        int idxPlatPayNo = xml.indexOf("<PlatPayNo>");
        int idxQsInfo = xml.indexOf("<qsInfo>");
        int idxSignElement = xml.indexOf("<SignElement>");
        int idxExt = xml.indexOf("<ExtInfo>");
        assertThat(idxSerial).isLessThan(idxPlatPayNo);
        assertThat(idxPlatPayNo).isLessThan(idxQsInfo);
        assertThat(idxQsInfo).isLessThan(idxSignElement);
        assertThat(idxSignElement).isLessThan(idxExt);

        PlatPay3115 parsed = unmarshal(xml, PlatPay3115.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3115-MAIN-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getDesNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getPlatPayNo()).isEqualTo("PLATPAY3115001");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南某某核心企业A");
        assertThat(parsed.getHxqyCode()).isEqualTo("91430100MA00000001");

        List<QsInfo> qsList = parsed.getQsInfo();
        assertThat(qsList).hasSize(2);

        QsInfo p1 = qsList.get(0);
        assertThat(p1.getQsSerialNo()).isEqualTo("QSSN3115-001");
        assertThat(p1.getPzNo()).isEqualTo("PZ3115001");
        assertThat(p1.getFkfAccName()).isEqualTo("付款方公司A");
        assertThat(p1.getFkfAccNo()).isEqualTo("6225880100000001");
        assertThat(p1.getSkfAccName()).isEqualTo("收款方公司A");
        assertThat(p1.getSkfAccNo()).isEqualTo("6225880100000002");
        assertThat(p1.getSkfBankCode()).isEqualTo("102100099996");
        assertThat(p1.getAmt()).isEqualTo("1000000.00");
        assertThat(p1.getWishDate()).isEqualTo("20260425");
        assertThat(p1.getQsPostscript()).isEqualTo("供应链清算款A");
        assertThat(p1.getQsMemo()).isEqualTo("月度结算");

        QsReturnInfo pr1 = p1.getQsReturnInfo();
        assertThat(pr1).isNotNull();
        assertThat(pr1.getQsReturnBankName()).isEqualTo("某某银行湖南分行");
        assertThat(pr1.getQsReturnCode()).isEqualTo("00");
        assertThat(pr1.getQsReturnSerialNo()).isEqualTo("BANKSN3115-001");
        assertThat(pr1.getQsReturnDate()).isEqualTo("20260424");
        assertThat(pr1.getQsReturnFilename()).isEqualTo("receipt-3115-001.pdf");
        assertThat(pr1.getQsReturnMemo()).isEqualTo("清算成功");

        QsInfo p2 = qsList.get(1);
        assertThat(p2.getQsSerialNo()).isEqualTo("QSSN3115-002");
        assertThat(p2.getPzNo()).isNull();
        assertThat(p2.getSkfBankCode()).isNull();
        assertThat(p2.getQsPostscript()).isNull();
        assertThat(p2.getQsMemo()).isNull();
        assertThat(p2.getQsReturnInfo()).isNull();
        assertThat(p2.getAmt()).isEqualTo("500000.00");

        assertThat(parsed.getSignElement())
                .isEqualTo("fkrAccName|fkrAccNo|skrAccName|skrAccNo|Amt|WishDate");
        assertThat(parsed.getQsfqSign()).isEqualTo("BASE64ENCODEDPK7SIGN==");
        assertThat(parsed.getPlatSign()).isEqualTo("BASE64ENCODEDPK7PLATSIGN==");

        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3115附加数据");
    }

    // ── BankCheckDay3116 ──────────────────────────────────────

    @Test
    void bankCheckDay3116_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(BankCheckDay3116.class)).isTrue();
    }

    @Test
    void bankCheckDay3116_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        // Detail #1 — full required + selected optional
        CheckDetailInfo d1 = new CheckDetailInfo();
        d1.setSid("1");
        d1.setPlatNodeCode(FepConstants.HNDEMP_NODE_CODE);
        d1.setPzNo("PZ3116001");
        d1.setBizType("01");
        d1.setBillNo("BILL3116-001");
        d1.setRzqyName("融资企业A");
        d1.setRzqyCode("91430100MA00000010");
        d1.setRzAmt("2000000.00");
        d1.setRzRate("0.0485");
        d1.setRzStartDate("20260101");
        d1.setRzEndDate("20261231");
        d1.setAmt("100000.00");
        d1.setRepayStyle("等额本息");
        d1.setLxAmt("8083.33");
        d1.setDbAmt("500.00");
        d1.setPlatServiceAmt("250.00");
        d1.setCheckMemo("月度还本付息");

        // Detail #2 — minimal required, all optional omitted
        CheckDetailInfo d2 = new CheckDetailInfo();
        d2.setSid("2");
        d2.setPlatNodeCode(FepConstants.HNDEMP_NODE_CODE);
        d2.setBizType("02");
        d2.setRzqyName("融资企业B");
        d2.setRzqyCode("91430100MA00000011");
        d2.setRzAmt("1500000.00");
        d2.setRzRate("0.0500");
        d2.setRzStartDate("20260301");
        d2.setRzEndDate("20270228");
        d2.setAmt("50000.00");
        // pzNo / BillNo / RepayStyle / lxAmt / dbAmt / PlatServiceAmt / CheckMemo all omitted

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3116附加数据");
        ext.setExtJsonFilename("ext3116.json");

        BankCheckDay3116 original = new BankCheckDay3116();
        original.setSerialNo("SN3116-MAIN-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setHxqyName("湖南某某核心企业A");
        original.setHxqyCode("91430100MA00000001");
        original.setCheckDate("20260424");
        original.setCheckDetailNum("2");
        original.setCheckDetailInfo(List.of(d1, d2));
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<BankCheckDay3116")
                .contains("<SerialNo>SN3116-MAIN-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNodeCode>")
                .contains("<hxqyName>湖南某某核心企业A</hxqyName>")
                .contains("<hxqyCode>91430100MA00000001</hxqyCode>")
                .contains("<CheckDate>20260424</CheckDate>")
                .contains("<CheckDetailNum>2</CheckDetailNum>")
                .contains("<CheckDetailInfo>")
                .contains("<sid>1</sid>")
                .contains("<PlatNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</PlatNodeCode>")
                .contains("<pzNo>PZ3116001</pzNo>")
                .contains("<BizType>01</BizType>")
                .contains("<BillNo>BILL3116-001</BillNo>")
                .contains("<rzqyName>融资企业A</rzqyName>")
                .contains("<rzqyCode>91430100MA00000010</rzqyCode>")
                .contains("<rzAmt>2000000.00</rzAmt>")
                .contains("<rzRate>0.0485</rzRate>")
                .contains("<rzStartDate>20260101</rzStartDate>")
                .contains("<rzEndDate>20261231</rzEndDate>")
                .contains("<Amt>100000.00</Amt>")
                .contains("<RepayStyle>等额本息</RepayStyle>")
                .contains("<lxAmt>8083.33</lxAmt>")
                .contains("<dbAmt>500.00</dbAmt>")
                .contains("<PlatServiceAmt>250.00</PlatServiceAmt>")
                .contains("<CheckMemo>月度还本付息</CheckMemo>")
                .contains("<sid>2</sid>")
                .contains("<rzqyName>融资企业B</rzqyName>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3116附加数据</ExtData>");

        // Field order check (XSD propOrder: SerialNo < CheckDate < CheckDetailNum < CheckDetailInfo < ExtInfo)
        int idxSerial = xml.indexOf("<SerialNo>");
        int idxCheckDate = xml.indexOf("<CheckDate>");
        int idxCheckDetailNum = xml.indexOf("<CheckDetailNum>");
        int idxCheckDetailInfo = xml.indexOf("<CheckDetailInfo>");
        int idxExt = xml.indexOf("<ExtInfo>");
        assertThat(idxSerial).isLessThan(idxCheckDate);
        assertThat(idxCheckDate).isLessThan(idxCheckDetailNum);
        assertThat(idxCheckDetailNum).isLessThan(idxCheckDetailInfo);
        assertThat(idxCheckDetailInfo).isLessThan(idxExt);

        BankCheckDay3116 parsed = unmarshal(xml, BankCheckDay3116.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3116-MAIN-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getHxqyName()).isEqualTo("湖南某某核心企业A");
        assertThat(parsed.getHxqyCode()).isEqualTo("91430100MA00000001");
        assertThat(parsed.getCheckDate()).isEqualTo("20260424");
        assertThat(parsed.getCheckDetailNum()).isEqualTo("2");

        List<CheckDetailInfo> dList = parsed.getCheckDetailInfo();
        assertThat(dList).hasSize(2);

        CheckDetailInfo pd1 = dList.get(0);
        assertThat(pd1.getSid()).isEqualTo("1");
        assertThat(pd1.getPlatNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(pd1.getPzNo()).isEqualTo("PZ3116001");
        assertThat(pd1.getBizType()).isEqualTo("01");
        assertThat(pd1.getBillNo()).isEqualTo("BILL3116-001");
        assertThat(pd1.getRzqyName()).isEqualTo("融资企业A");
        assertThat(pd1.getRzqyCode()).isEqualTo("91430100MA00000010");
        assertThat(pd1.getRzAmt()).isEqualTo("2000000.00");
        assertThat(pd1.getRzRate()).isEqualTo("0.0485");
        assertThat(pd1.getRzStartDate()).isEqualTo("20260101");
        assertThat(pd1.getRzEndDate()).isEqualTo("20261231");
        assertThat(pd1.getAmt()).isEqualTo("100000.00");
        assertThat(pd1.getRepayStyle()).isEqualTo("等额本息");
        assertThat(pd1.getLxAmt()).isEqualTo("8083.33");
        assertThat(pd1.getDbAmt()).isEqualTo("500.00");
        assertThat(pd1.getPlatServiceAmt()).isEqualTo("250.00");
        assertThat(pd1.getCheckMemo()).isEqualTo("月度还本付息");

        CheckDetailInfo pd2 = dList.get(1);
        assertThat(pd2.getSid()).isEqualTo("2");
        assertThat(pd2.getPlatNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(pd2.getPzNo()).isNull();
        assertThat(pd2.getBizType()).isEqualTo("02");
        assertThat(pd2.getBillNo()).isNull();
        assertThat(pd2.getRzqyName()).isEqualTo("融资企业B");
        assertThat(pd2.getRzqyCode()).isEqualTo("91430100MA00000011");
        assertThat(pd2.getRzAmt()).isEqualTo("1500000.00");
        assertThat(pd2.getAmt()).isEqualTo("50000.00");
        assertThat(pd2.getRepayStyle()).isNull();
        assertThat(pd2.getLxAmt()).isNull();
        assertThat(pd2.getDbAmt()).isNull();
        assertThat(pd2.getPlatServiceAmt()).isNull();
        assertThat(pd2.getCheckMemo()).isNull();

        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3116附加数据");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3116.json");
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
