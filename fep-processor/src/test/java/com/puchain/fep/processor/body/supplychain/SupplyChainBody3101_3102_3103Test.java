package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import com.puchain.fep.processor.body.common.FileInfo;
import com.puchain.fep.processor.body.common.PersonInfo;
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
 * JAXB marshal/unmarshal roundtrip tests for the 3101 → 3102 → 3103 contract-archive
 * triad supply-chain Body POJOs and their support type {@link RzqyBaseInfo}.
 *
 * <p>Verifies field counts: {@link ContractInfo3101} (23), {@link ArchiveInfo3102}
 * (18 with 6 nested objects), {@link ArchiveReturnInfo3103} (20). The 3102 nesting
 * exercises {@link RzqyBaseInfo} (9), {@link QyAccInfo} (4), {@link PersonInfo} (8)
 * and {@code List<FileInfo>} ({@code maxOccurs="10"}).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3101_3102_3103Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    ContractInfo3101.class, ArchiveInfo3102.class, ArchiveReturnInfo3103.class,
                    RzqyBaseInfo.class, QyAccInfo.class, PersonInfo.class, FileInfo.class,
                    ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    void contractInfo3101_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(ContractInfo3101.class)).isTrue();
    }

    @Test
    void archiveInfo3102_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(ArchiveInfo3102.class)).isTrue();
    }

    @Test
    void archiveReturnInfo3103_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(ArchiveReturnInfo3103.class)).isTrue();
    }

    @Test
    void contractInfo3101_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3101扩展");
        ext.setExtJsonFilename("ext3101.json");

        ContractInfo3101 original = new ContractInfo3101();
        original.setSerialNo("SN3101-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setContractNo("HT-2026042400001");
        original.setHxqyCode("91430000MA4L7DXY01");
        original.setContractType("供应链融资合同");
        original.setDigitalSeal("1");
        original.setContractFilename("contract.pdf");
        original.setCertFilename("cert.pem");
        original.setJfqyName("湖南某某甲方企业");
        original.setJfqyCode("91430100MA4L7DXY02");
        original.setYfqyName("湖南某某乙方企业");
        original.setYfqyCode("91430100MA4L7DXY03");
        original.setQyName3("湖南某某第三方企业");
        original.setQyCode3("91430100MA4L7DXY04");
        original.setQyName4("湖南某某第四方企业");
        original.setQyCode4("91430100MA4L7DXY05");
        original.setSxDate("20260424");
        original.setQzDate("20260423");
        original.setYwValue1("YW1-VALUE");
        original.setYwValue2("YW2-VALUE");
        original.setContractReturnMemo("合同附言备注");
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<ContractInfo3101")
                .contains("<SerialNo>SN3101-001</SerialNo>")
                .contains("<SendNodeCode>B1001010203</SendNodeCode>")
                .contains("<DesNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNodeCode>")
                .contains("<ContractNo>HT-2026042400001</ContractNo>")
                .contains("<hxqyCode>91430000MA4L7DXY01</hxqyCode>")
                .contains("<ContractType>供应链融资合同</ContractType>")
                .contains("<DigitalSeal>1</DigitalSeal>")
                .contains("<ContractFilename>contract.pdf</ContractFilename>")
                .contains("<CertFilename>cert.pem</CertFilename>")
                .contains("<jfqyName>湖南某某甲方企业</jfqyName>")
                .contains("<jfqyCode>91430100MA4L7DXY02</jfqyCode>")
                .contains("<yfqyName>湖南某某乙方企业</yfqyName>")
                .contains("<yfqyCode>91430100MA4L7DXY03</yfqyCode>")
                .contains("<qyName3>湖南某某第三方企业</qyName3>")
                .contains("<qyCode3>91430100MA4L7DXY04</qyCode3>")
                .contains("<qyName4>湖南某某第四方企业</qyName4>")
                .contains("<qyCode4>91430100MA4L7DXY05</qyCode4>")
                .contains("<sxDate>20260424</sxDate>")
                .contains("<qzDate>20260423</qzDate>")
                .contains("<ywValue1>YW1-VALUE</ywValue1>")
                .contains("<ywValue2>YW2-VALUE</ywValue2>")
                .contains("<ContractReturnMemo>合同附言备注</ContractReturnMemo>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3101扩展</ExtData>");

        ContractInfo3101 parsed = unmarshal(xml, ContractInfo3101.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3101-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getContractNo()).isEqualTo("HT-2026042400001");
        assertThat(parsed.getHxqyCode()).isEqualTo("91430000MA4L7DXY01");
        assertThat(parsed.getContractType()).isEqualTo("供应链融资合同");
        assertThat(parsed.getDigitalSeal()).isEqualTo("1");
        assertThat(parsed.getContractFilename()).isEqualTo("contract.pdf");
        assertThat(parsed.getCertFilename()).isEqualTo("cert.pem");
        assertThat(parsed.getJfqyName()).isEqualTo("湖南某某甲方企业");
        assertThat(parsed.getJfqyCode()).isEqualTo("91430100MA4L7DXY02");
        assertThat(parsed.getYfqyName()).isEqualTo("湖南某某乙方企业");
        assertThat(parsed.getYfqyCode()).isEqualTo("91430100MA4L7DXY03");
        assertThat(parsed.getQyName3()).isEqualTo("湖南某某第三方企业");
        assertThat(parsed.getQyCode3()).isEqualTo("91430100MA4L7DXY04");
        assertThat(parsed.getQyName4()).isEqualTo("湖南某某第四方企业");
        assertThat(parsed.getQyCode4()).isEqualTo("91430100MA4L7DXY05");
        assertThat(parsed.getSxDate()).isEqualTo("20260424");
        assertThat(parsed.getQzDate()).isEqualTo("20260423");
        assertThat(parsed.getYwValue1()).isEqualTo("YW1-VALUE");
        assertThat(parsed.getYwValue2()).isEqualTo("YW2-VALUE");
        assertThat(parsed.getContractReturnMemo()).isEqualTo("合同附言备注");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3101扩展");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3101.json");
    }

    @Test
    void archiveInfo3102_jaxbRoundtrip_shouldPreserveAllFieldsAndNestedBlocks() throws Exception {
        // RzqyBaseInfo (9 fields, all populated)
        RzqyBaseInfo baseInfo = new RzqyBaseInfo();
        baseInfo.setQyCreditCode("ZZ20260424001");
        baseInfo.setZjRgstDate("20200101");
        baseInfo.setZjExpDate("20300101");
        baseInfo.setQyType("01");
        baseInfo.setQyClass("F66");
        baseInfo.setQySize("0001");
        baseInfo.setRegAddr("长沙市岳麓区某某路1号");
        baseInfo.setPostAddr("长沙市岳麓区某某路1号A栋");
        baseInfo.setMailAddr("contact@example.com");

        // QyAccInfo (4 fields)
        QyAccInfo accInfo = new QyAccInfo();
        accInfo.setAccName("湖南某某融资企业");
        accInfo.setAccNumber("6228480000000001234");
        accInfo.setAccBankName("中国工商银行长沙分行");
        accInfo.setAccBankCode("ICBKCNBJHNL");

        // PersonInfo for operator (8 fields)
        PersonInfo operator = new PersonInfo();
        operator.setName("张三");
        operator.setCertType("01");
        operator.setCertNumber("430102198001011234");
        operator.setCertStartDate("20200101");
        operator.setCertEndDate("20300101");
        operator.setPhone("13800001111");
        operator.setPostAddr("长沙市岳麓区某某路1号");
        operator.setMailAddr("zhangsan@example.com");

        // PersonInfo for legal representative (8 fields)
        PersonInfo legal = new PersonInfo();
        legal.setName("李四");
        legal.setCertType("01");
        legal.setCertNumber("430102197001011234");
        legal.setCertStartDate("20100101");
        legal.setCertEndDate("20300101");
        legal.setPhone("13800002222");
        legal.setPostAddr("长沙市岳麓区某某路2号");
        legal.setMailAddr("lisi@example.com");

        // List<FileInfo> with 2 entries (within 0..10 maxOccurs)
        FileInfo file1 = new FileInfo();
        file1.setFileType("01");
        file1.setFilename("营业执照.pdf");
        file1.setFileMemo("企业营业执照");
        FileInfo file2 = new FileInfo();
        file2.setFileType("02");
        file2.setFilename("法人身份证.pdf");
        file2.setFileMemo("法人身份证扫描件");

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3102扩展");
        ext.setExtJsonFilename("ext3102.json");

        ArchiveInfo3102 original = new ArchiveInfo3102();
        original.setSerialNo("SN3102-001");
        original.setSendNodeCode("B1001010203");
        original.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setApplyMode("1");
        original.setGroupName("某某集团");
        original.setGroupCode("91430000MA4L7DXY99");
        original.setHxqyName("湖南某某核心企业");
        original.setHxqyCode("91430000MA4L7DXY01");
        original.setRzqyName("湖南某某融资企业");
        original.setRzqyCode("91430000MA4L7DXY10");
        original.setRzqyPlatNo("PLAT00001");
        original.setRzqyCAFilename("ca.cer");
        original.setRzqyBaseInfo(baseInfo);
        original.setRzqyAccInfo(accInfo);
        original.setRzqyOperatorInfo(operator);
        original.setRzqyLegalInfo(legal);
        original.setAttachFileInfo(List.of(file1, file2));
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<ArchiveInfo3102")
                .contains("<SerialNo>SN3102-001</SerialNo>")
                .contains("<ApplyMode>1</ApplyMode>")
                .contains("<GroupName>某某集团</GroupName>")
                .contains("<GroupCode>91430000MA4L7DXY99</GroupCode>")
                .contains("<hxqyName>湖南某某核心企业</hxqyName>")
                .contains("<hxqyCode>91430000MA4L7DXY01</hxqyCode>")
                .contains("<rzqyName>湖南某某融资企业</rzqyName>")
                .contains("<rzqyCode>91430000MA4L7DXY10</rzqyCode>")
                .contains("<rzqyPlatNo>PLAT00001</rzqyPlatNo>")
                .contains("<rzqyCAFilename>ca.cer</rzqyCAFilename>")
                .contains("<rzqyBaseInfo>")
                .contains("<qyCreditCode>ZZ20260424001</qyCreditCode>")
                .contains("<zjRgstDate>20200101</zjRgstDate>")
                .contains("<zjExpDate>20300101</zjExpDate>")
                .contains("<qyType>01</qyType>")
                .contains("<qyClass>F66</qyClass>")
                .contains("<qySize>0001</qySize>")
                .contains("<RegAddr>长沙市岳麓区某某路1号</RegAddr>")
                .contains("<rzqyAccInfo>")
                .contains("<AccName>湖南某某融资企业</AccName>")
                .contains("<AccNumber>6228480000000001234</AccNumber>")
                .contains("<rzqyOperatorInfo>")
                .contains("<Name>张三</Name>")
                .contains("<Phone>13800001111</Phone>")
                .contains("<rzqyLegalInfo>")
                .contains("<Name>李四</Name>")
                .contains("<Phone>13800002222</Phone>")
                .contains("<AttachFileInfo>")
                .contains("<Filename>营业执照.pdf</Filename>")
                .contains("<Filename>法人身份证.pdf</Filename>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3102扩展</ExtData>");

        ArchiveInfo3102 parsed = unmarshal(xml, ArchiveInfo3102.class);
        // 12 simple fields
        assertThat(parsed.getSerialNo()).isEqualTo("SN3102-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getApplyMode()).isEqualTo("1");
        assertThat(parsed.getGroupName()).isEqualTo("某某集团");
        assertThat(parsed.getGroupCode()).isEqualTo("91430000MA4L7DXY99");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南某某核心企业");
        assertThat(parsed.getHxqyCode()).isEqualTo("91430000MA4L7DXY01");
        assertThat(parsed.getRzqyName()).isEqualTo("湖南某某融资企业");
        assertThat(parsed.getRzqyCode()).isEqualTo("91430000MA4L7DXY10");
        assertThat(parsed.getRzqyPlatNo()).isEqualTo("PLAT00001");
        assertThat(parsed.getRzqyCAFilename()).isEqualTo("ca.cer");

        // RzqyBaseInfo 9 fields
        assertThat(parsed.getRzqyBaseInfo()).isNotNull();
        RzqyBaseInfo parsedBase = parsed.getRzqyBaseInfo();
        assertThat(parsedBase.getQyCreditCode()).isEqualTo("ZZ20260424001");
        assertThat(parsedBase.getZjRgstDate()).isEqualTo("20200101");
        assertThat(parsedBase.getZjExpDate()).isEqualTo("20300101");
        assertThat(parsedBase.getQyType()).isEqualTo("01");
        assertThat(parsedBase.getQyClass()).isEqualTo("F66");
        assertThat(parsedBase.getQySize()).isEqualTo("0001");
        assertThat(parsedBase.getRegAddr()).isEqualTo("长沙市岳麓区某某路1号");
        assertThat(parsedBase.getPostAddr()).isEqualTo("长沙市岳麓区某某路1号A栋");
        assertThat(parsedBase.getMailAddr()).isEqualTo("contact@example.com");

        // QyAccInfo 4 fields
        assertThat(parsed.getRzqyAccInfo()).isNotNull();
        assertThat(parsed.getRzqyAccInfo().getAccName()).isEqualTo("湖南某某融资企业");
        assertThat(parsed.getRzqyAccInfo().getAccNumber()).isEqualTo("6228480000000001234");
        assertThat(parsed.getRzqyAccInfo().getAccBankName()).isEqualTo("中国工商银行长沙分行");
        assertThat(parsed.getRzqyAccInfo().getAccBankCode()).isEqualTo("ICBKCNBJHNL");

        // operator PersonInfo 8 fields
        assertThat(parsed.getRzqyOperatorInfo()).isNotNull();
        assertThat(parsed.getRzqyOperatorInfo().getName()).isEqualTo("张三");
        assertThat(parsed.getRzqyOperatorInfo().getPhone()).isEqualTo("13800001111");
        assertThat(parsed.getRzqyOperatorInfo().getCertNumber()).isEqualTo("430102198001011234");

        // legal PersonInfo
        assertThat(parsed.getRzqyLegalInfo()).isNotNull();
        assertThat(parsed.getRzqyLegalInfo().getName()).isEqualTo("李四");
        assertThat(parsed.getRzqyLegalInfo().getPhone()).isEqualTo("13800002222");

        // AttachFileInfo List<FileInfo> (2 entries)
        assertThat(parsed.getAttachFileInfo()).hasSize(2);
        assertThat(parsed.getAttachFileInfo().get(0).getFilename()).isEqualTo("营业执照.pdf");
        assertThat(parsed.getAttachFileInfo().get(0).getFileType()).isEqualTo("01");
        assertThat(parsed.getAttachFileInfo().get(1).getFilename()).isEqualTo("法人身份证.pdf");

        // ExtInfo
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3102扩展");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3102.json");
    }

    @Test
    void archiveReturnInfo3103_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3103扩展");
        ext.setExtJsonFilename("ext3103.json");

        ArchiveReturnInfo3103 original = new ArchiveReturnInfo3103();
        original.setSerialNo("SN3103-001");
        original.setSendNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setDesNodeCode("B1001010203");
        original.setCreationRetCode("11");
        original.setCreationRetInfo("建档成功");
        original.setHxqyName("湖南某某核心企业");
        original.setHxqyCode("91430000MA4L7DXY01");
        original.setRzqyName("湖南某某融资企业");
        original.setRzqyCode("91430000MA4L7DXY10");
        original.setRzqyBankCusCode("CUS-2026042400001");
        original.setBranchBankName("中国工商银行长沙岳麓支行");
        original.setCusManagerName("王五");
        original.setCusManagerPhone("13800003333");
        original.setRzAmt("5000000.00");
        original.setRzRate("0.045000");
        original.setSxRate("0.001500");
        original.setDbRate("0.002000");
        original.setStartDate("20260424");
        original.setEndDate("20270424");
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<ArchiveReturnInfo3103")
                .contains("<SerialNo>SN3103-001</SerialNo>")
                .contains("<SendNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendNodeCode>")
                .contains("<DesNodeCode>B1001010203</DesNodeCode>")
                .contains("<CreationRetCode>11</CreationRetCode>")
                .contains("<CreationRetInfo>建档成功</CreationRetInfo>")
                .contains("<hxqyName>湖南某某核心企业</hxqyName>")
                .contains("<hxqyCode>91430000MA4L7DXY01</hxqyCode>")
                .contains("<rzqyName>湖南某某融资企业</rzqyName>")
                .contains("<rzqyCode>91430000MA4L7DXY10</rzqyCode>")
                .contains("<rzqyBankCusCode>CUS-2026042400001</rzqyBankCusCode>")
                .contains("<BranchBankName>中国工商银行长沙岳麓支行</BranchBankName>")
                .contains("<CusManagerName>王五</CusManagerName>")
                .contains("<CusManagerPhone>13800003333</CusManagerPhone>")
                .contains("<rzAmt>5000000.00</rzAmt>")
                .contains("<rzRate>0.045000</rzRate>")
                .contains("<sxRate>0.001500</sxRate>")
                .contains("<dbRate>0.002000</dbRate>")
                .contains("<StartDate>20260424</StartDate>")
                .contains("<EndDate>20270424</EndDate>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3103扩展</ExtData>");

        ArchiveReturnInfo3103 parsed = unmarshal(xml, ArchiveReturnInfo3103.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3103-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getDesNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getCreationRetCode()).isEqualTo("11");
        assertThat(parsed.getCreationRetInfo()).isEqualTo("建档成功");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南某某核心企业");
        assertThat(parsed.getHxqyCode()).isEqualTo("91430000MA4L7DXY01");
        assertThat(parsed.getRzqyName()).isEqualTo("湖南某某融资企业");
        assertThat(parsed.getRzqyCode()).isEqualTo("91430000MA4L7DXY10");
        assertThat(parsed.getRzqyBankCusCode()).isEqualTo("CUS-2026042400001");
        assertThat(parsed.getBranchBankName()).isEqualTo("中国工商银行长沙岳麓支行");
        assertThat(parsed.getCusManagerName()).isEqualTo("王五");
        assertThat(parsed.getCusManagerPhone()).isEqualTo("13800003333");
        assertThat(parsed.getRzAmt()).isEqualTo("5000000.00");
        assertThat(parsed.getRzRate()).isEqualTo("0.045000");
        assertThat(parsed.getSxRate()).isEqualTo("0.001500");
        assertThat(parsed.getDbRate()).isEqualTo("0.002000");
        assertThat(parsed.getStartDate()).isEqualTo("20260424");
        assertThat(parsed.getEndDate()).isEqualTo("20270424");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3103扩展");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3103.json");
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
