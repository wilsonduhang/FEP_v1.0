package com.puchain.fep.processor.body.common;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MsgReturn9120}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MsgReturn9120Test {

    @Test
    void shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(MsgReturn9120.class)).isTrue();
    }

    @Test
    void jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        MsgReturn9120 original = new MsgReturn9120();
        original.setOriMsgNo("3001");
        original.setDebug("test debug info");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<MsgReturn9120")
                .contains("<OriMsgNo>3001</OriMsgNo>")
                .contains("<Debug>test debug info</Debug>");

        MsgReturn9120 parsed = JaxbRoundtripSupport.unmarshal(xml, MsgReturn9120.class);
        assertThat(parsed.getOriMsgNo()).isEqualTo("3001");
        assertThat(parsed.getDebug()).isEqualTo("test debug info");
    }

    @Test
    void optionalDebug_shouldBeOmittedWhenNull() throws Exception {
        MsgReturn9120 minimal = new MsgReturn9120();
        minimal.setOriMsgNo("3002");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .contains("<OriMsgNo>3002</OriMsgNo>")
                .doesNotContain("<Debug>");
    }

    @Test
    void xsdValidation_shouldPass_forValid9120Envelope() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<CFX>\n"
                + "    <HEAD>\n"
                + "        <Version>1.0</Version>\n"
                + "        <SrcNode>10000000000001</SrcNode>\n"
                + "        <DesNode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNode>\n"
                + "        <App>HNDEMP</App>\n"
                + "        <MsgNo>9120</MsgNo>\n"
                + "        <MsgId>20260416103000000001</MsgId>\n"
                + "        <CorrMsgId>20260416103000000000</CorrMsgId>\n"
                + "        <WorkDate>20260416</WorkDate>\n"
                + "    </HEAD>\n"
                + "    <MSG>\n"
                + "        <BatchHead9120>\n"
                + "            <SendOrgCode>10000000000001</SendOrgCode>\n"
                + "            <EntrustDate>20260416</EntrustDate>\n"
                + "            <TransitionNo>00000001</TransitionNo>\n"
                + "            <Result>90000</Result>\n"
                + "        </BatchHead9120>\n"
                + "        <MsgReturn9120>\n"
                + "            <OriMsgNo>3001</OriMsgNo>\n"
                + "        </MsgReturn9120>\n"
                + "    </MSG>\n"
                + "</CFX>";

        XsdValidator validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
        ValidationResult result = validator.validate(
                MessageType.MSG_9120, xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }
}
