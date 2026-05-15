package com.puchain.fep.processor.body.common;

import com.puchain.fep.common.util.FepConstants;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-2 (2026-05-07): 文本块内嵌入字面量 "A1000143000104" 是 HNDEMP 中心节点代码 fixture，与
 * {@link com.puchain.fep.common.util.FepConstants#HNDEMP_NODE_CODE} 同源。Java 文本块语法
 * (JEP 378) 不支持中段插入常量引用，故保留字面量于 fixture XML；新写测试请 import
 * {@code FepConstants} 并仅在 Java 表达式上下文中引用。
 */
class PzInfoTest {

    private final XsdValidator xsdValidator = AbstractXsdValidationTest.SHARED_VALIDATOR;

    // ==================== helpers ====================

    private PzInfo buildMinimalPzInfo() {
        PzInfo pz = new PzInfo();
        pz.setPlatShortName("PLATA");
        pz.setPlatCode("91110000000000100X");
        pz.setExternalPlat("01");
        pz.setHxqyName("核心企业A");
        pz.setHxqyCode("91110000000000001X");
        pz.setPzNo("PZ20260421000001");
        pz.setPzClass("01");
        pz.setPzFunction("001");                  // XSD Number3 length=3
        pz.setKlzrfName("开立方B");
        pz.setKlzrfCode("91110000000000002X");
        pz.setJsqyName("接收企业C");
        pz.setJsqyCode("91110000000000003X");
        pz.setJsqyPlatNo("PLATC00001");           // XSD rzqyPlatCode minLen=10
        pz.setPzAmt("100000.00");
        pz.setPzStartDate("20260421");
        pz.setPzEndDate("20260521");
        pz.setPzState("01");
        pz.setPzrzState("01");
        pz.setPzFlowNum("1");
        return pz;
    }

    private PzInfoReturn3004 buildMinimalResponseWithPzInfo(PzInfo pz) {
        PzInfoReturn3004 body = new PzInfoReturn3004();
        body.setSerialNo("SN2026042100000000000000000001");   // XSD length=30
        body.setSendNodeCode(FepConstants.HNDEMP_NODE_CODE);               // XSD length=14
        body.setDesNodeCode("10000000000001");
        body.setHxqyName("核心企业A");
        body.setHxqyCode("91110000000000001X");
        body.setPzNo("PZ20260421000001");
        body.setPzState("01");
        body.setPzrzState("01");
        PzrzStatusInfo status = new PzrzStatusInfo();
        status.setPzNo("PZ20260421000001");
        status.setRzPhaseCode("01");
        status.setBankNodeCode("10000000000001");
        body.setPzrzStatusInfo(status);
        body.setPzInfo(pz);
        return body;
    }

    private byte[] marshalFullCfx(PzInfoReturn3004 body, MessageType type) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(PzInfoReturn3004.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter sw = new StringWriter();
        m.marshal(body, sw);
        String bodyXml = sw.toString();
        String cfx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version><SrcNode>A1000143000104</SrcNode>
                <DesNode>10000000000001</DesNode><App>HNDEMP</App>
                <MsgNo>%s</MsgNo><MsgId>20260421103000000001</MsgId>
                <CorrMsgId>20260421103000000000</CorrMsgId><WorkDate>20260421</WorkDate>
              </HEAD>
              <MSG>
                <RealHead3004>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>00000</Result>
                </RealHead3004>
                %s
              </MSG>
            </CFX>
            """.formatted(type.msgNo(), bodyXml);
        return cfx.getBytes(StandardCharsets.UTF_8);
    }

    private PzFlowInfo buildPzFlowInfo(int sn, String pzNo) {
        PzFlowInfo f = new PzFlowInfo();
        f.setSerialNumber(String.valueOf(sn));
        f.setPzNo(pzNo);
        f.setPreNo("PZ20260420000000");
        f.setQyAssignName("转让方A");              // XSD qyName minLen=2
        f.setQyAssignCode("91110000000000001X");
        f.setQyRecvName("接收方B");
        f.setQyRecvCode("91110000000000002X");
        f.setAmt("100.00");
        f.setUpdateDate("20260421");
        return f;
    }

    private String marshal(PzInfo pz) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(PzInfo.class);
        StringWriter sw = new StringWriter();
        ctx.createMarshaller().marshal(pz, sw);
        return sw.toString();
    }

    // ==================== 测试 ====================

    @Test
    void marshalAndUnmarshal_pzFlowInfoList_roundtrip() throws Exception {
        PzFlowInfo f1 = buildPzFlowInfo(1, "PZ20260421000001");
        PzFlowInfo f2 = buildPzFlowInfo(2, "PZ20260421000002");
        PzInfo pz = buildMinimalPzInfo();
        pz.setPzFlowInfo(List.of(f1, f2));
        // propOrder 验证需 pzrzSubAmt/pzFilename 非 null 以生成元素
        pz.setPzrzSubAmt("100.00");
        pz.setPzFilename("cert.pdf");

        String xml = marshal(pz);

        assertThat(xml).containsPattern("<pzFlowInfo>[\\s\\S]*<SerialNumber>1</SerialNumber>[\\s\\S]*</pzFlowInfo>");
        assertThat(xml).containsPattern("<pzFlowInfo>[\\s\\S]*<SerialNumber>2</SerialNumber>[\\s\\S]*</pzFlowInfo>");
        // propOrder 位置验证：pzFlowInfo 在 pzrzSubAmt 和 pzFilename 之间
        assertThat(xml.indexOf("<pzrzSubAmt>")).isLessThan(xml.indexOf("<pzFlowInfo>"));
        assertThat(xml.indexOf("<pzFlowInfo>")).isLessThan(xml.indexOf("<pzFilename>"));

        JAXBContext ctx = JAXBContext.newInstance(PzInfo.class);
        PzInfo rt = (PzInfo) ctx.createUnmarshaller().unmarshal(new StringReader(xml));
        assertThat(rt.getPzFlowInfo()).hasSize(2);
        assertThat(rt.getPzFlowInfo().get(0).getPzNo()).isEqualTo("PZ20260421000001");
        assertThat(rt.getPzFlowInfo().get(1).getSerialNumber()).isEqualTo("2");
    }

    @Test
    void marshal_pzFlowInfoNull_omitsElement() throws Exception {
        PzInfo pz = buildMinimalPzInfo();
        pz.setPzFlowInfo(null);
        String xml = marshal(pz);
        assertThat(xml).doesNotContain("<pzFlowInfo>");
    }

    @Test
    void marshal_pzFlowInfoEmptyList_omitsElement() throws Exception {
        PzInfo pz = buildMinimalPzInfo();
        pz.setPzFlowInfo(List.of());
        String xml = marshal(pz);
        assertThat(xml).doesNotContain("<pzFlowInfo>");
    }

    @Test
    void xsdValidation_validPzInfoWithOnePzFlowInfo_succeeds() throws Exception {
        PzInfo pz = buildMinimalPzInfo();
        pz.setPzFlowInfo(List.of(buildPzFlowInfo(1, "PZ20260421000001")));
        PzInfoReturn3004 body = buildMinimalResponseWithPzInfo(pz);
        byte[] xml = marshalFullCfx(body, MessageType.MSG_3004);
        ValidationResult result = xsdValidator.validate(MessageType.MSG_3004, xml);
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void xsdValidation_elevenPzFlowInfo_exceedsMaxOccurs_fails() throws Exception {
        PzInfo pz = buildMinimalPzInfo();
        List<PzFlowInfo> list = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            list.add(buildPzFlowInfo(i, "PZ" + String.format("%010d", i)));
        }
        pz.setPzFlowInfo(list);
        PzInfoReturn3004 body = buildMinimalResponseWithPzInfo(pz);
        byte[] xml = marshalFullCfx(body, MessageType.MSG_3004);

        ValidationResult result = xsdValidator.validate(MessageType.MSG_3004, xml);
        assertThat(result.valid()).isFalse();
        // errors() 返回 List<String>
        assertThat(result.errors())
                .anyMatch(s -> s.toLowerCase().contains("maxoccurs")
                            || s.contains("cvc-complex-type"));
    }
}
