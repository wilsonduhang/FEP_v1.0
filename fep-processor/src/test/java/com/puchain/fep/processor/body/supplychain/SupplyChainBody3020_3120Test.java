package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests for 3020 {@link Forward3020} (real-time
 * forward) and 3120 {@link Forward3120} (non-real-time forward) supply chain
 * Body POJOs.
 *
 * <p>3020 与 3120 字段集相同（7 字段），仅 Content 必填性不同；分别测两类的
 * extendsCfxBody + roundtrip 共 4 cases。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3020_3120Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    Forward3020.class, Forward3120.class, ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── Forward3020 ─────────────────────────────────────────────

    @Test
    void forward3020_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(Forward3020.class)).isTrue();
    }

    @Test
    void forward3020_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3020附加数据");

        Forward3020 original = new Forward3020();
        original.setSerialNo("SN3020-001");
        original.setSrcNodeCode("B1001010203");
        original.setDesNodeCode("A1000143000104");
        original.setBusinessNo("BIZ-3020-01");
        original.setParameters("param1=value1&param2=value2");
        original.setContent("<payload>3020 real-time forward content</payload>");
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<Forward3020")
                .contains("<SerialNo>SN3020-001</SerialNo>")
                .contains("<SrcNodeCode>B1001010203</SrcNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<BusinessNo>BIZ-3020-01</BusinessNo>")
                .contains("<Parameters>param1=value1&amp;param2=value2</Parameters>")
                .contains("<Content>")
                .contains("3020 real-time forward content")
                .contains("<ExtInfo>")
                .contains("<ExtData>3020附加数据</ExtData>");

        Forward3020 parsed = unmarshal(xml, Forward3020.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3020-001");
        assertThat(parsed.getSrcNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getBusinessNo()).isEqualTo("BIZ-3020-01");
        assertThat(parsed.getParameters()).isEqualTo("param1=value1&param2=value2");
        assertThat(parsed.getContent()).contains("3020 real-time forward content");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3020附加数据");
    }

    // ── Forward3120 ─────────────────────────────────────────────

    @Test
    void forward3120_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(Forward3120.class)).isTrue();
    }

    @Test
    void forward3120_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        ExtInfo ext = new ExtInfo();
        ext.setExtData("3120附加数据");

        Forward3120 original = new Forward3120();
        original.setSerialNo("SN3120-001");
        original.setSrcNodeCode("B1001010203");
        original.setDesNodeCode("A1000143000104");
        original.setBusinessNo("BIZ-3120-01");
        original.setParameters("batchId=20260424001");
        original.setContent("<payload>3120 batch forward content (required)</payload>");
        original.setExtInfo(ext);

        String xml = marshal(original);
        assertThat(xml)
                .contains("<Forward3120")
                .contains("<SerialNo>SN3120-001</SerialNo>")
                .contains("<SrcNodeCode>B1001010203</SrcNodeCode>")
                .contains("<DesNodeCode>A1000143000104</DesNodeCode>")
                .contains("<BusinessNo>BIZ-3120-01</BusinessNo>")
                .contains("<Parameters>batchId=20260424001</Parameters>")
                .contains("<Content>")
                .contains("3120 batch forward content (required)")
                .contains("<ExtInfo>")
                .contains("<ExtData>3120附加数据</ExtData>");

        Forward3120 parsed = unmarshal(xml, Forward3120.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3120-001");
        assertThat(parsed.getSrcNodeCode()).isEqualTo("B1001010203");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getBusinessNo()).isEqualTo("BIZ-3120-01");
        assertThat(parsed.getParameters()).isEqualTo("batchId=20260424001");
        assertThat(parsed.getContent()).contains("3120 batch forward content (required)");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3120附加数据");
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
