package com.puchain.fep.converter.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

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
        return CfxMessage.of(head, null);
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
        // jaxb-runtime 4.x 默认 ValidationEventHandler 会吞掉非 FATAL_ERROR 事件。
        // 必须注册严格 handler (event -> false) 让 setter 抛的 IllegalArgumentException
        // 升级为 UnmarshalException 的 rootCause。Task 5 的 XmlCodec 会在业务侧统一强制这一点。
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
}
