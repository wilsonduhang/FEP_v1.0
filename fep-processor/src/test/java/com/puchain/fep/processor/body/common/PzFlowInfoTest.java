package com.puchain.fep.processor.body.common;

import jakarta.xml.bind.JAXBContext;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PzFlowInfoTest {

    @Test
    void marshalAndUnmarshal_allFields_roundtrip() throws Exception {
        PzFlowInfo o = build(true);
        JAXBContext ctx = JAXBContext.newInstance(PzFlowInfo.class);
        StringWriter sw = new StringWriter();
        ctx.createMarshaller().marshal(o, sw);
        String xml = sw.toString();

        assertThat(xml).contains("<SerialNumber>1</SerialNumber>");
        assertThat(xml).contains("<pzNo>PZ20260421000001</pzNo>");
        assertThat(xml).contains("<AssignSign>BASE64SIGNDATA==</AssignSign>");

        PzFlowInfo rt = (PzFlowInfo) ctx.createUnmarshaller().unmarshal(new StringReader(xml));
        assertThat(rt.getSerialNumber()).isEqualTo("1");
        assertThat(rt.getAssignSign()).isEqualTo("BASE64SIGNDATA==");
        assertThat(rt.getFlowVoucherFile()).isEqualTo("flow-20260421-001.pdf");
    }

    @Test
    void marshal_onlyRequiredFields_omitsOptionals() throws Exception {
        PzFlowInfo o = build(false);
        JAXBContext ctx = JAXBContext.newInstance(PzFlowInfo.class);
        StringWriter sw = new StringWriter();
        ctx.createMarshaller().marshal(o, sw);
        String xml = sw.toString();

        assertThat(xml).doesNotContain("AssignSign");
        assertThat(xml).doesNotContain("FlowVoucherFile");
    }

    @Test
    void unmarshal_wellFormedXml_populatesAllFields() throws Exception {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pzFlowInfo>
              <SerialNumber>2</SerialNumber>
              <pzNo>PZ20260422000001</pzNo>
              <PreNo>PZ20260421000001</PreNo>
              <qyAssignName>转让方A</qyAssignName>
              <qyAssignCode>91110000000000001X</qyAssignCode>
              <qyRecvName>接收方B</qyRecvName>
              <qyRecvCode>91110000000000002X</qyRecvCode>
              <Amt>100.00</Amt>
              <UpdateDate>20260422</UpdateDate>
            </pzFlowInfo>
            """;
        JAXBContext ctx = JAXBContext.newInstance(PzFlowInfo.class);
        PzFlowInfo o = (PzFlowInfo) ctx.createUnmarshaller().unmarshal(new StringReader(xml));
        assertThat(o.getSerialNumber()).isEqualTo("2");
        assertThat(o.getQyRecvCode()).isEqualTo("91110000000000002X");
        assertThat(o.getAssignSign()).isNull();
    }

    @Test
    void buildList_tenItems_serializableBoundary() {
        List<PzFlowInfo> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            PzFlowInfo o = build(false);
            o.setSerialNumber(String.valueOf(i));
            o.setPzNo("PZ" + String.format("%010d", i));
            list.add(o);
        }
        assertThat(list).hasSize(10);
        assertThat(list.get(0).getSerialNumber()).isEqualTo("1");
        assertThat(list.get(9).getSerialNumber()).isEqualTo("10");
    }

    @Test
    void unmarshal_requiredOnly_populatesFields() throws Exception {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <pzFlowInfo>
              <SerialNumber>1</SerialNumber>
              <pzNo>PZ20260421000001</pzNo>
              <PreNo>PZ20260420000001</PreNo>
              <qyAssignName>转让方A</qyAssignName>
              <qyAssignCode>91110000000000001X</qyAssignCode>
              <qyRecvName>接收方B</qyRecvName>
              <qyRecvCode>91110000000000002X</qyRecvCode>
              <Amt>100000.00</Amt>
              <UpdateDate>20260421</UpdateDate>
            </pzFlowInfo>
            """;
        JAXBContext ctx = JAXBContext.newInstance(PzFlowInfo.class);
        PzFlowInfo o = (PzFlowInfo) ctx.createUnmarshaller().unmarshal(new StringReader(xml));
        assertThat(o.getPzNo()).isEqualTo("PZ20260421000001");
    }

    private PzFlowInfo build(boolean withOptionals) {
        PzFlowInfo o = new PzFlowInfo();
        o.setSerialNumber("1");
        o.setPzNo("PZ20260421000001");
        o.setPreNo("PZ20260421000000");
        o.setQyAssignName("转让方A");
        o.setQyAssignCode("91110000000000001X");
        o.setQyRecvName("接收方B");
        o.setQyRecvCode("91110000000000002X");
        o.setAmt("100000.00");
        o.setUpdateDate("20260421");
        if (withOptionals) {
            o.setAssignSign("BASE64SIGNDATA==");
            o.setFlowVoucherFile("flow-20260421-001.pdf");
        }
        return o;
    }
}
