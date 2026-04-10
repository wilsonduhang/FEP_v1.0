package com.puchain.fep.converter.xml;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * XmlCodec 单元测试。验证 marshal/unmarshal 行为、UTF-8 XML 声明、
 * 严格 ValidationEventHandler 对 setter 校验异常的升级路径。
 */
class XmlCodecTest {

    private XmlCodec codec;

    @BeforeEach
    void setUp() {
        codec = new XmlCodec();
    }

    private CommonHead sampleHead() {
        CommonHead head = new CommonHead();
        head.setSrcNode("12345678901234");
        head.setDesNode("A1000143000104");
        head.setMsgNo("3101");
        head.setMsgId("20260410120000000001");
        head.setCorrMsgId("20260410120000000001");
        head.setWorkDate("20260410");
        return head;
    }

    @Test
    void marshalShouldStartWithUtf8XmlDeclaration() {
        String xml = codec.marshal(CfxMessage.of(sampleHead(), null));
        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    @Test
    void marshalShouldNotIncludeStandaloneAttribute() {
        String xml = codec.marshal(CfxMessage.of(sampleHead(), null));
        assertThat(xml).doesNotContain("standalone");
    }

    @Test
    void roundTripShouldPreserveAllHeadFields() {
        String xml = codec.marshal(CfxMessage.of(sampleHead(), null));
        CfxMessage restored = codec.unmarshal(xml);

        CommonHead h = restored.getHead();
        assertThat(h.getVersion()).isEqualTo("1.0");
        assertThat(h.getApp()).isEqualTo("HNDEMP");
        assertThat(h.getSrcNode()).isEqualTo("12345678901234");
        assertThat(h.getDesNode()).isEqualTo("A1000143000104");
        assertThat(h.getMsgNo()).isEqualTo("3101");
        assertThat(h.getMsgId()).isEqualTo("20260410120000000001");
        assertThat(h.getCorrMsgId()).isEqualTo("20260410120000000001");
        assertThat(h.getWorkDate()).isEqualTo("20260410");
    }

    @Test
    void unmarshalInvalidXmlShouldRaiseConv8002() {
        assertThatThrownBy(() -> codec.unmarshal("<<NOT XML"))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8002));
    }

    @Test
    void unmarshalMalformedMsgIdShouldRaiseConv8002ViaSetterValidation() {
        // 回归 Plan v1.1 fix #2 + jaxb-runtime 妥协方案：XmlCodec 安装严格
        // ValidationEventHandler，让 setter 抛的 IllegalArgumentException
        // 升级为 UnmarshalException，最终被 XmlCodec 包装为 CONV_8002。
        String bad = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX><HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>12345678901234</SrcNode>"
                + "<DesNode>A1000143000104</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>3101</MsgNo>"
                + "<MsgId>TOO_SHORT</MsgId>"
                + "<CorrMsgId>20260410120000000001</CorrMsgId>"
                + "<WorkDate>20260410</WorkDate>"
                + "</HEAD><MSG/></CFX>";
        assertThatThrownBy(() -> codec.unmarshal(bad))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8002));
    }

    @Test
    void repeatedMarshalShouldProduceIdenticalOutput() {
        // 健康检查：JAXBContext 在构造器中初始化一次，多次 marshal 结果一致
        XmlCodec c = new XmlCodec();
        String xml1 = c.marshal(CfxMessage.of(sampleHead(), null));
        String xml2 = c.marshal(CfxMessage.of(sampleHead(), null));
        assertThat(xml1).isEqualTo(xml2);
    }
}
