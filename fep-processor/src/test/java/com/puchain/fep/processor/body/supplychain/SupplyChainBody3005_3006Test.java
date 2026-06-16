package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.common.util.FepConstants;
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
 * JAXB marshal/unmarshal roundtrip tests for 3005 {@link QyAccQuery3005}
 * and 3006 {@link QyAccQueryReturn3006} supply chain Body POJOs.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainBody3005_3006Test {

    private static final JAXBContext CTX;

    static {
        try {
            CTX = JAXBContext.newInstance(
                    QyAccQuery3005.class, QyAccQueryReturn3006.class,
                    ExtInfo.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ── QyAccQuery3005 ─────────────────────────────────────

    @Test
    void qyAccQuery3005_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(QyAccQuery3005.class)).isTrue();
    }

    @Test
    void qyAccQuery3005_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        QyAccQuery3005 original = new QyAccQuery3005();
        original.setSerialNo("SN3005-001");
        original.setSendNodeCode("12345678901234");
        original.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setQyAccName("湖南测试企业有限公司");
        original.setQyAccCode("123456789012345678");

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3005附加数据");
        ext.setExtJsonFilename("ext3005.json");
        original.setExtInfo(ext);

        String xml = marshal(original);

        assertThat(xml)
                .contains("<qyAccQuery3005")
                .contains("<SerialNo>SN3005-001</SerialNo>")
                .contains("<SendNodeCode>12345678901234</SendNodeCode>")
                .contains("<DesNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNodeCode>")
                .contains("<qyAccName>湖南测试企业有限公司</qyAccName>")
                .contains("<qyAccCode>123456789012345678</qyAccCode>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3005附加数据</ExtData>")
                .contains("<ExtJSONFilename>ext3005.json</ExtJSONFilename>");

        QyAccQuery3005 parsed = unmarshal(xml, QyAccQuery3005.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3005-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("12345678901234");
        assertThat(parsed.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getQyAccName()).isEqualTo("湖南测试企业有限公司");
        assertThat(parsed.getQyAccCode()).isEqualTo("123456789012345678");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3005附加数据");
        assertThat(parsed.getExtInfo().getExtJsonFilename()).isEqualTo("ext3005.json");
    }

    @Test
    void qyAccQuery3005_optionalExtInfo_shouldBeOmittedWhenNull() throws Exception {
        QyAccQuery3005 minimal = new QyAccQuery3005();
        minimal.setSerialNo("SN3005-002");
        minimal.setSendNodeCode("12345678901234");
        minimal.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        minimal.setQyAccName("最小测试企业");
        minimal.setQyAccCode("123456789012345678");

        String xml = marshal(minimal);

        assertThat(xml).doesNotContain("<ExtInfo>");
    }

    // ── QyAccQueryReturn3006 ───────────────────────────────

    @Test
    void qyAccQueryReturn3006_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(QyAccQueryReturn3006.class)).isTrue();
    }

    @Test
    void qyAccQueryReturn3006_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        QyAccQueryReturn3006 original = new QyAccQueryReturn3006();
        original.setSerialNo("SN3006-001");
        original.setSendNodeCode("12345678901234");
        original.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        original.setQyAccName("湖南测试企业有限公司");
        original.setQyAccCode("123456789012345678");
        original.setAccReturnCode("0");
        original.setAccReturnMemo("查询成功");

        ExtInfo ext = new ExtInfo();
        ext.setExtData("3006附加数据");
        original.setExtInfo(ext);

        String xml = marshal(original);

        assertThat(xml)
                .contains("<qyAccQueryReturn3006")
                .contains("<SerialNo>SN3006-001</SerialNo>")
                .contains("<SendNodeCode>12345678901234</SendNodeCode>")
                .contains("<DesNodeCode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNodeCode>")
                .contains("<qyAccName>湖南测试企业有限公司</qyAccName>")
                .contains("<qyAccCode>123456789012345678</qyAccCode>")
                .contains("<AccReturnCode>0</AccReturnCode>")
                .contains("<AccReturnMemo>查询成功</AccReturnMemo>")
                .contains("<ExtInfo>")
                .contains("<ExtData>3006附加数据</ExtData>");

        QyAccQueryReturn3006 parsed = unmarshal(xml, QyAccQueryReturn3006.class);
        assertThat(parsed.getSerialNo()).isEqualTo("SN3006-001");
        assertThat(parsed.getSendNodeCode()).isEqualTo("12345678901234");
        assertThat(parsed.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(parsed.getQyAccName()).isEqualTo("湖南测试企业有限公司");
        assertThat(parsed.getQyAccCode()).isEqualTo("123456789012345678");
        assertThat(parsed.getAccReturnCode()).isEqualTo("0");
        assertThat(parsed.getAccReturnMemo()).isEqualTo("查询成功");
        assertThat(parsed.getExtInfo()).isNotNull();
        assertThat(parsed.getExtInfo().getExtData()).isEqualTo("3006附加数据");
    }

    @Test
    void qyAccQueryReturn3006_optionalFields_shouldBeOmittedWhenNull() throws Exception {
        QyAccQueryReturn3006 minimal = new QyAccQueryReturn3006();
        minimal.setSerialNo("SN3006-002");
        minimal.setSendNodeCode("12345678901234");
        minimal.setDesNodeCode(FepConstants.HNDEMP_NODE_CODE);
        minimal.setQyAccName("最小测试企业");
        minimal.setQyAccCode("123456789012345678");
        minimal.setAccReturnCode("1");

        String xml = marshal(minimal);

        assertThat(xml)
                .doesNotContain("<AccReturnMemo>")
                .doesNotContain("<ExtInfo>");

        QyAccQueryReturn3006 parsed = unmarshal(xml, QyAccQueryReturn3006.class);
        assertThat(parsed.getAccReturnMemo()).isNull();
        assertThat(parsed.getExtInfo()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────

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
