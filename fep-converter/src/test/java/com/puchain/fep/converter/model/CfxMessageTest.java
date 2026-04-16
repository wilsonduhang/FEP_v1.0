package com.puchain.fep.converter.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CfxMessageTest {

    private CfxMessage buildHeadOnlyMessage() {
        CommonHead head = new CommonHead();
        head.setSrcNode("12345678901234");
        head.setDesNode("A1000143000104");
        head.setMsgNo("1001");
        head.setMsgId("20260410120000000001");
        head.setCorrMsgId("20260410120000000001");
        head.setWorkDate("20260410");
        return CfxMessage.of(head, (Object[]) null);
    }

    @Test
    void marshalledCfxRoot_shouldContainHeadAndMsg() throws Exception {
        CfxMessage msg = buildHeadOnlyMessage();

        JAXBContext ctx = JAXBContext.newInstance(CfxMessage.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        StringWriter sw = new StringWriter();
        m.marshal(msg, sw);

        String xml = sw.toString();
        assertThat(xml).contains("<CFX>").contains("<HEAD>").contains("</HEAD>");
        assertThat(xml).containsPattern("<MSG\\s*/>|<MSG></MSG>");
        assertThat(xml.indexOf("<HEAD>")).isPositive();
        assertThat(xml.indexOf("<HEAD>")).isLessThan(xml.indexOf("<MSG"));
        assertThat(xml).contains("<Version>1.0</Version>");
        assertThat(xml).contains("<App>HNDEMP</App>");
        assertThat(xml).contains("<SrcNode>12345678901234</SrcNode>");
    }

    @Test
    void commonHead_msgIdMustBe20Chars() {
        CommonHead head = new CommonHead();
        assertThatThrownBy(() -> head.setMsgId("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MsgId");
    }

    @Test
    void unmarshalWithMalformedMsgId_shouldRaiseViaSetterValidation() throws Exception {
        String bad = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX><HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>12345678901234</SrcNode>"
                + "<DesNode>A1000143000104</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>1001</MsgNo>"
                + "<MsgId>TOO_SHORT</MsgId>"
                + "<CorrMsgId>20260410120000000001</CorrMsgId>"
                + "<WorkDate>20260410</WorkDate>"
                + "</HEAD><MSG/></CFX>";
        JAXBContext ctx = JAXBContext.newInstance(CfxMessage.class);
        Unmarshaller u = ctx.createUnmarshaller();
        u.setEventHandler(event -> false);
        assertThatThrownBy(() ->
                u.unmarshal(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8))))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void msgContainerWithBody_shouldSerializeBodyContent() throws Exception {
        CommonHead head = new CommonHead();
        head.setSrcNode("12345678901234");
        head.setDesNode("A1000143000104");
        head.setMsgNo("9005");
        head.setMsgId("20260410120000000001");
        head.setCorrMsgId("20260410120000000001");
        head.setWorkDate("20260410");

        org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        org.w3c.dom.Element bodyElem = doc.createElement("Ping");
        bodyElem.setTextContent("hello");

        CfxMessage msg = CfxMessage.of(head, bodyElem);

        JAXBContext ctx = JAXBContext.newInstance(CfxMessage.class);
        Marshaller m = ctx.createMarshaller();
        StringWriter sw = new StringWriter();
        m.marshal(msg, sw);
        String xml = sw.toString();

        assertThat(xml).contains("<MSG>").contains("<Ping>hello</Ping>").contains("</MSG>");
    }

    /**
     * P1b-DEFECT-001: MsgContainer must capture multiple children under MSG.
     */
    @Test
    void unmarshal_msgWithTwoChildren_shouldCaptureBoth() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX><HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>12345678901234</SrcNode>"
                + "<DesNode>A1000143000104</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>1001</MsgNo>"
                + "<MsgId>20260410120000000001</MsgId>"
                + "<CorrMsgId>20260410120000000001</CorrMsgId>"
                + "<WorkDate>20260410</WorkDate>"
                + "</HEAD><MSG>"
                + "<ChildA><Name>Alice</Name></ChildA>"
                + "<ChildB><Name>Bob</Name></ChildB>"
                + "</MSG></CFX>";

        JAXBContext ctx = JAXBContext.newInstance(CfxMessage.class);
        Unmarshaller u = ctx.createUnmarshaller();
        CfxMessage msg = (CfxMessage) u.unmarshal(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        List<Object> bodies = msg.getBodies();
        assertThat(bodies).hasSize(2);
        assertThat(bodies.get(0)).isInstanceOf(org.w3c.dom.Element.class);
        assertThat(bodies.get(1)).isInstanceOf(org.w3c.dom.Element.class);

        org.w3c.dom.Element first = (org.w3c.dom.Element) bodies.get(0);
        org.w3c.dom.Element second = (org.w3c.dom.Element) bodies.get(1);
        assertThat(first.getTagName()).isEqualTo("ChildA");
        assertThat(second.getTagName()).isEqualTo("ChildB");
    }

    @Test
    void marshal_msgWithTwoBodies_shouldSerializeBoth() throws Exception {
        CommonHead head = new CommonHead();
        head.setSrcNode("12345678901234");
        head.setDesNode("A1000143000104");
        head.setMsgNo("9005");
        head.setMsgId("20260410120000000001");
        head.setCorrMsgId("20260410120000000001");
        head.setWorkDate("20260410");

        org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        org.w3c.dom.Element elem1 = doc.createElement("ReqHead");
        elem1.setTextContent("data1");
        org.w3c.dom.Element elem2 = doc.createElement("ReqBody");
        elem2.setTextContent("data2");

        CfxMessage msg = CfxMessage.of(head, elem1, elem2);

        JAXBContext ctx = JAXBContext.newInstance(CfxMessage.class);
        Marshaller m = ctx.createMarshaller();
        StringWriter sw = new StringWriter();
        m.marshal(msg, sw);
        String xml = sw.toString();

        assertThat(xml).contains("<MSG>");
        assertThat(xml).contains("<ReqHead>data1</ReqHead>");
        assertThat(xml).contains("<ReqBody>data2</ReqBody>");
        assertThat(xml).contains("</MSG>");
    }

    @Test
    void getBody_singleElement_shouldReturnIt() {
        org.w3c.dom.Document doc;
        try {
            doc = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        org.w3c.dom.Element elem = doc.createElement("Single");

        CfxMessage msg = CfxMessage.of(new CommonHead(), elem);
        assertThat(msg.getBody()).isSameAs(elem);
        assertThat(msg.getBodies()).hasSize(1);
    }

    @Test
    void getBody_empty_shouldReturnNull() {
        CfxMessage msg = CfxMessage.of(new CommonHead(), (Object[]) null);
        assertThat(msg.getBody()).isNull();
        assertThat(msg.getBodies()).isEmpty();
    }
}
