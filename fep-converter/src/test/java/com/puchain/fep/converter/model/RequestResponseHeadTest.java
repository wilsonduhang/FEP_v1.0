package com.puchain.fep.converter.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 单元测试：{@link RequestResponseHead} 3020/3115/3120 通用转发报文双向业务头 POJO。
 *
 * <p>覆盖 10 用例，验证 {@code Result}/{@code AddWord} 可选 (XSD minOccurs=0)
 * 的 null-passthrough 语义，区别于 {@link ResponseBusinessHead} 的 5 位数字强校验。</p>
 *
 * <ol>
 *   <li>getterSetterRoundtrip — 基础 getter/setter 往返</li>
 *   <li>resultNullPassthrough — setResult(null) 不校验 (核心设计差异点)</li>
 *   <li>addWordNullPassthrough — setAddWord(null) 不校验 (核心设计差异点)</li>
 *   <li>extendsRequestBusinessHeadInheritsSendOrgCode — 继承父类 3 字段</li>
 *   <li>marshalEmptyResultProducesNoResultElement — Result null → 不输出 element</li>
 *   <li>marshalFilledResultProducesResultElement — Result 非 null → 输出 element</li>
 *   <li>marshalNullAddWordOmitsElement — AddWord null → 不输出 element</li>
 *   <li>roundtripWithResultAndAddWord — marshal + unmarshal 全字段往返</li>
 *   <li>roundtripWithoutOptionalFields — 仅父类 3 字段往返</li>
 *   <li>propOrderForJaxbContextRegistration — JAXBContext 注册不抛 IllegalAnnotationsException</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RequestResponseHeadTest {

    @Test
    void getterSetterRoundtrip() {
        RequestResponseHead h = new RequestResponseHead();
        h.setResult("RESPONSE-OK");
        h.setAddWord("通用转发应答附言");
        assertThat(h.getResult()).isEqualTo("RESPONSE-OK");
        assertThat(h.getAddWord()).isEqualTo("通用转发应答附言");
    }

    @Test
    void resultNullPassthrough() {
        RequestResponseHead h = new RequestResponseHead();
        h.setResult(null);
        assertThat(h.getResult()).isNull();
    }

    @Test
    void addWordNullPassthrough() {
        RequestResponseHead h = new RequestResponseHead();
        h.setAddWord(null);
        assertThat(h.getAddWord()).isNull();
    }

    @Test
    void extendsRequestBusinessHeadInheritsSendOrgCode() {
        RequestResponseHead h = new RequestResponseHead();
        h.setSendOrgCode("10000000000001");
        h.setEntrustDate("20260515");
        h.setTransitionNo("00000007");
        assertThat(h).isInstanceOf(RequestBusinessHead.class);
        assertThat(h.getSendOrgCode()).isEqualTo("10000000000001");
        assertThat(h.getEntrustDate()).isEqualTo("20260515");
        assertThat(h.getTransitionNo()).isEqualTo("00000007");
    }

    @Test
    void marshalEmptyResultProducesNoResultElement() throws Exception {
        RequestResponseHead h = new RequestResponseHead();
        h.setSendOrgCode("10000000000001");
        h.setEntrustDate("20260515");
        h.setTransitionNo("00000007");
        // Result null

        String xml = marshal(h);

        assertThat(xml).doesNotContain("<Result>");
        assertThat(xml).contains("<SendOrgCode>10000000000001</SendOrgCode>");
    }

    @Test
    void marshalFilledResultProducesResultElement() throws Exception {
        RequestResponseHead h = new RequestResponseHead();
        h.setSendOrgCode("10000000000001");
        h.setEntrustDate("20260515");
        h.setTransitionNo("00000007");
        h.setResult("FORWARD-ACCEPTED");

        String xml = marshal(h);

        assertThat(xml).contains("<Result>FORWARD-ACCEPTED</Result>");
    }

    @Test
    void marshalNullAddWordOmitsElement() throws Exception {
        RequestResponseHead h = new RequestResponseHead();
        h.setSendOrgCode("10000000000001");
        h.setEntrustDate("20260515");
        h.setTransitionNo("00000007");
        h.setResult("OK");
        // AddWord null

        String xml = marshal(h);

        assertThat(xml).doesNotContain("<AddWord>");
        assertThat(xml).contains("<Result>OK</Result>");
    }

    @Test
    void roundtripWithResultAndAddWord() throws Exception {
        RequestResponseHead h = new RequestResponseHead();
        h.setSendOrgCode("10000000000001");
        h.setEntrustDate("20260515");
        h.setTransitionNo("00000007");
        h.setResult("FWD-200");
        h.setAddWord("通用转发处理完成");

        JAXBContext ctx = JAXBContext.newInstance(RequestResponseHead.class);
        String xml = marshal(ctx, h);
        RequestResponseHead back = unmarshal(ctx, xml);

        assertThat(back.getSendOrgCode()).isEqualTo("10000000000001");
        assertThat(back.getEntrustDate()).isEqualTo("20260515");
        assertThat(back.getTransitionNo()).isEqualTo("00000007");
        assertThat(back.getResult()).isEqualTo("FWD-200");
        assertThat(back.getAddWord()).isEqualTo("通用转发处理完成");
    }

    @Test
    void roundtripWithoutOptionalFields() throws Exception {
        RequestResponseHead h = new RequestResponseHead();
        h.setSendOrgCode("10000000000001");
        h.setEntrustDate("20260515");
        h.setTransitionNo("00000007");

        JAXBContext ctx = JAXBContext.newInstance(RequestResponseHead.class);
        String xml = marshal(ctx, h);
        RequestResponseHead back = unmarshal(ctx, xml);

        assertThat(back.getSendOrgCode()).isEqualTo("10000000000001");
        assertThat(back.getResult()).isNull();
        assertThat(back.getAddWord()).isNull();
    }

    @Test
    void propOrderForJaxbContextRegistration() {
        // RED BAR: subclass propOrder must list ONLY own properties; including parent's
        // sendOrgCode/entrustDate/transitionNo triggers IllegalAnnotationsException.
        assertThatCode(() -> JAXBContext.newInstance(RequestResponseHead.class))
                .doesNotThrowAnyException();
    }

    /** RequestResponseHead 无 @XmlRootElement（envelope 片段范式，同 ResponseBusinessHead），
     *  以 JAXBElement + QName 包裹后才能作为根元素 marshal/unmarshal。 */
    private static final QName ROOT = new QName("RequestResponseHead");

    private static String marshal(final RequestResponseHead h) throws Exception {
        return marshal(JAXBContext.newInstance(RequestResponseHead.class), h);
    }

    private static String marshal(final JAXBContext ctx, final RequestResponseHead h)
            throws Exception {
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        StringWriter sw = new StringWriter();
        m.marshal(new JAXBElement<>(ROOT, RequestResponseHead.class, h), sw);
        return sw.toString();
    }

    private static RequestResponseHead unmarshal(final JAXBContext ctx, final String xml)
            throws Exception {
        Unmarshaller u = ctx.createUnmarshaller();
        return u.unmarshal(
                new StreamSource(new ByteArrayInputStream(
                        xml.getBytes(StandardCharsets.UTF_8))),
                RequestResponseHead.class).getValue();
    }
}
