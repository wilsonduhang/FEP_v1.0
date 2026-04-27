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

/**
 * 单元测试：{@link RealHead9008} 9008 节点登出请求业务头 POJO。
 *
 * <p>覆盖 4 用例（镜像 {@link RealHead9006Test}）：</p>
 * <ol>
 *   <li>roundtrip: marshal + unmarshal 3 字段往返保持</li>
 *   <li>minimumRequired: 仅设最小 required 字段，3 元素全部出现 + propOrder 顺序</li>
 *   <li>setterValidation: 3 字段非法值各抛 {@link IllegalArgumentException} 含字段名</li>
 *   <li>nullFields: null 透传不抛错（与 {@link RequestBusinessHead} 行为一致）</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RealHead9008Test {

    @Test
    void roundtrip_shouldPreserve3Fields() throws Exception {
        RealHead9008 h = new RealHead9008();
        h.setSendOrgCode("10000000000001");
        h.setEntrustDate("20260427");
        h.setTransitionNo("00000002");

        JAXBContext ctx = JAXBContext.newInstance(RealHead9008.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        StringWriter sw = new StringWriter();
        m.marshal(h, sw);
        String xml = sw.toString();

        assertThat(xml)
                .contains("<RealHead9008")
                .contains("<SendOrgCode>10000000000001</SendOrgCode>")
                .contains("<EntrustDate>20260427</EntrustDate>")
                .contains("<TransitionNo>00000002</TransitionNo>");
        // propOrder 验证：SendOrgCode → EntrustDate → TransitionNo
        assertThat(xml.indexOf("<SendOrgCode>"))
                .isLessThan(xml.indexOf("<EntrustDate>"));
        assertThat(xml.indexOf("<EntrustDate>"))
                .isLessThan(xml.indexOf("<TransitionNo>"));

        Unmarshaller u = ctx.createUnmarshaller();
        RealHead9008 back = (RealHead9008) u.unmarshal(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThat(back.getSendOrgCode()).isEqualTo("10000000000001");
        assertThat(back.getEntrustDate()).isEqualTo("20260427");
        assertThat(back.getTransitionNo()).isEqualTo("00000002");
    }

    @Test
    void minimumRequired_shouldMarshalAllThreeFields() throws Exception {
        RealHead9008 h = new RealHead9008();
        h.setSendOrgCode("99999999999999");
        h.setEntrustDate("20260101");
        h.setTransitionNo("88888888");

        JAXBContext ctx = JAXBContext.newInstance(RealHead9008.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        StringWriter sw = new StringWriter();
        m.marshal(h, sw);
        String xml = sw.toString();

        assertThat(xml).contains("<SendOrgCode>99999999999999</SendOrgCode>");
        assertThat(xml).contains("<EntrustDate>20260101</EntrustDate>");
        assertThat(xml).contains("<TransitionNo>88888888</TransitionNo>");
    }

    @Test
    void setterValidation_shouldRejectMalformedFields() {
        RealHead9008 h = new RealHead9008();
        assertThatThrownBy(() -> h.setSendOrgCode("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SendOrgCode");
        assertThatThrownBy(() -> h.setEntrustDate("2026-04-27"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EntrustDate");
        assertThatThrownBy(() -> h.setTransitionNo("ABCD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TransitionNo");
    }

    @Test
    void nullFields_shouldBeAccepted() {
        RealHead9008 h = new RealHead9008();
        h.setSendOrgCode(null);
        h.setEntrustDate(null);
        h.setTransitionNo(null);
        assertThat(h.getSendOrgCode()).isNull();
        assertThat(h.getEntrustDate()).isNull();
        assertThat(h.getTransitionNo()).isNull();
    }
}
