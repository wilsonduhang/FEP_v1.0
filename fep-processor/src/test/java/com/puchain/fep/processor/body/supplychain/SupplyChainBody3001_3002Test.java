package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3001 {@link ProgressQuery3001}
 * and 3002 {@link ProgressQueryReturn3002} supply chain Body POJOs.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3001_3002Test {

    // ── ProgressQuery3001 ─────────────────────────────────

    @Test
    void progressQuery3001_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(ProgressQuery3001.class)).isTrue();
    }

    @Test
    void progressQuery3001_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        ProgressQuery3001 original = new ProgressQuery3001();
        original.setSerialNo("SN001");
        original.setSendNodeCode("12345678901234");
        original.setDesNodeCode("A1000143000104");
        original.setHxqyName("湖南核心企业有限公司");
        original.setHxqyCode("123456789012345678");
        original.setQueryType("01");
        original.setQueryKey("KEY20260416001");

        ExtInfo ext = new ExtInfo();
        ext.setExtData("附加数据");
        ext.setExtJsonFilename("ext3001.json");
        original.setExtInfo(ext);

        String xml = marshalWithExtInfo(original);

        assertThat(xml)
                .contains("<ProgressQuery3001")
                .contains("<SerialNo>SN001</SerialNo>")
                .contains("<SendNodeCode>12345678901234</SendNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<hxqyName>湖南核心企业有限公司</hxqyName>")
                .contains("<hxqyCode>123456789012345678</hxqyCode>")
                .contains("<QueryType>01</QueryType>")
                .contains("<QueryKey>KEY20260416001</QueryKey>")
                .contains("<ExtInfo>")
                .contains("<ExtData>附加数据</ExtData>")
                .contains("<ExtJSONFilename>ext3001.json</ExtJSONFilename>");

        ProgressQuery3001 parsed = unmarshalWithExtInfo(xml, ProgressQuery3001.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("12345678901234");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南核心企业有限公司");
        assertThat(parsed.getHxqyCode()).isEqualTo("123456789012345678");
        assertThat(parsed.getQueryType()).isEqualTo("01");
        assertThat(parsed.getQueryKey()).isEqualTo("KEY20260416001");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("附加数据");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3001.json");
    }

    @Test
    void progressQuery3001_optionalExtInfo_shouldBeOmittedWhenNull() throws Exception {
        ProgressQuery3001 minimal = new ProgressQuery3001();
        minimal.setSerialNo("SN002");
        minimal.setSendNodeCode("12345678901234");
        minimal.setDesNodeCode("A1000143000104");
        minimal.setHxqyName("最小测试企业");
        minimal.setHxqyCode("123456789012345678");
        minimal.setQueryType("02");
        minimal.setQueryKey("KEY20260416002");

        String xml = marshalWithExtInfo(minimal);

        assertThat(xml).doesNotContain("<ExtInfo>");
    }

    // ── ProgressQueryReturn3002 ───────────────────────────

    @Test
    void progressQueryReturn3002_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(ProgressQueryReturn3002.class)).isTrue();
    }

    @Test
    void progressQueryReturn3002_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        ProgressQueryReturn3002 original = new ProgressQueryReturn3002();
        original.setSerialNo("SN001");
        original.setSendNodeCode("12345678901234");
        original.setDesNodeCode("A1000143000104");
        original.setHxqyName("湖南核心企业有限公司");
        original.setHxqyCode("123456789012345678");
        original.setQueryType("01");
        original.setQueryKey("KEY20260416001");
        original.setReturnCode("0000");
        original.setReturnMemo("查询成功");

        ExtInfo ext = new ExtInfo();
        ext.setExtData("回执附加数据");
        original.setExtInfo(ext);

        String xml = marshalWithExtInfo(original);

        assertThat(xml)
                .contains("<ProgressQueryReturn3002")
                .contains("<SerialNo>SN001</SerialNo>")
                .contains("<SendNodeCode>12345678901234</SendNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<hxqyName>湖南核心企业有限公司</hxqyName>")
                .contains("<hxqyCode>123456789012345678</hxqyCode>")
                .contains("<QueryType>01</QueryType>")
                .contains("<QueryKey>KEY20260416001</QueryKey>")
                .contains("<ReturnCode>0000</ReturnCode>")
                .contains("<ReturnMemo>查询成功</ReturnMemo>")
                .contains("<ExtInfo>")
                .contains("<ExtData>回执附加数据</ExtData>");

        ProgressQueryReturn3002 parsed = unmarshalWithExtInfo(xml, ProgressQueryReturn3002.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("12345678901234");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getHxqyName()).isEqualTo("湖南核心企业有限公司");
        assertThat(parsed.getHxqyCode()).isEqualTo("123456789012345678");
        assertThat(parsed.getQueryType()).isEqualTo("01");
        assertThat(parsed.getQueryKey()).isEqualTo("KEY20260416001");
        assertThat(parsed.getReturnCode()).isEqualTo("0000");
        assertThat(parsed.getReturnMemo()).isEqualTo("查询成功");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("回执附加数据");
    }

    @Test
    void progressQueryReturn3002_optionalFields_shouldBeOmittedWhenNull() throws Exception {
        ProgressQueryReturn3002 minimal = new ProgressQueryReturn3002();
        minimal.setSerialNo("SN003");
        minimal.setSendNodeCode("12345678901234");
        minimal.setDesNodeCode("A1000143000104");
        minimal.setHxqyName("最小测试企业");
        minimal.setHxqyCode("123456789012345678");
        minimal.setQueryType("02");
        minimal.setQueryKey("KEY20260416003");
        minimal.setReturnCode("9999");

        String xml = marshalWithExtInfo(minimal);

        assertThat(xml)
                .doesNotContain("<ReturnMemo>")
                .doesNotContain("<ExtInfo>");
    }

    // ── helpers (include ExtInfo in JAXBContext) ───────────

    /**
     * Marshal with a JAXBContext that includes {@link ExtInfo} so nested elements are handled.
     */
    private <T> String marshalWithExtInfo(final T instance) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(instance.getClass(), ExtInfo.class);
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(instance, writer);
        return writer.toString();
    }

    /**
     * Unmarshal with a JAXBContext that includes {@link ExtInfo}.
     */
    @SuppressWarnings("unchecked")
    private <T> T unmarshalWithExtInfo(final String xml, final Class<T> type) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(type, ExtInfo.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        return (T) unmarshaller.unmarshal(new StringReader(xml));
    }
}
