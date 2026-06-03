package com.puchain.fep.converter.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试：{@link RealHead9005} 9005 节点心跳业务头 POJO（镜像 {@link RealHead9006Test}）。
 *
 * <p>覆盖 roundtrip（marshal + unmarshal 3 字段往返保持）+ propOrder 顺序 + 根元素名
 * {@code RealHead9005}（head-only 心跳报文区分于 9006）。字段约束（length / pattern）由父类
 * {@link AbstractRealHead} 校验，已在 {@link RealHead9006Test} 充分覆盖，本类不重复。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RealHead9005Test {

    @Test
    void roundtrip_shouldPreserve3Fields() throws Exception {
        RealHead9005 h = new RealHead9005();
        h.setSendOrgCode("10000000000001");
        h.setEntrustDate("20260421");
        h.setTransitionNo("00000005");

        JAXBContext ctx = JAXBContext.newInstance(RealHead9005.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        StringWriter sw = new StringWriter();
        m.marshal(h, sw);
        String xml = sw.toString();

        assertThat(xml)
                .contains("<RealHead9005")
                .contains("<SendOrgCode>10000000000001</SendOrgCode>")
                .contains("<EntrustDate>20260421</EntrustDate>")
                .contains("<TransitionNo>00000005</TransitionNo>");
        // propOrder 验证：SendOrgCode → EntrustDate → TransitionNo
        assertThat(xml.indexOf("<SendOrgCode>"))
                .isLessThan(xml.indexOf("<EntrustDate>"));
        assertThat(xml.indexOf("<EntrustDate>"))
                .isLessThan(xml.indexOf("<TransitionNo>"));

        Unmarshaller u = ctx.createUnmarshaller();
        RealHead9005 back = (RealHead9005) u.unmarshal(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThat(back.getSendOrgCode()).isEqualTo("10000000000001");
        assertThat(back.getEntrustDate()).isEqualTo("20260421");
        assertThat(back.getTransitionNo()).isEqualTo("00000005");
    }
}
