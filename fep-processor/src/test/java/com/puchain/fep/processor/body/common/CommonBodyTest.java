package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests + toString masking assertions for
 * common Body POJOs covering PRD v1.3 §4.5 node workflow and generic
 * forward/ack messages.
 *
 * <p>Covered POJOs:
 * <ul>
 *   <li>{@link LoginRequest9006} — Password / NewPassword (both masked in toString).</li>
 *   <li>{@link LoginResponse9007} — Status (non-sensitive).</li>
 *   <li>{@link LogoutRequest9008} — Password (masked in toString).</li>
 *   <li>{@link LogoutResponse9009} — Status (non-sensitive).</li>
 *   <li>{@link Forward9000} — 实时业务通用转发 (6 fields, BusinessNo optional).</li>
 *   <li>{@link Forward9100} — 非实时业务通用转发 (6 fields, BusinessNo optional).</li>
 *   <li>{@link MsgReturn9020} — 实时业务通用应答 (OriMsgNo + optional Debug).</li>
 * </ul></p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CommonBodyTest {

    // ── CfxBody inheritance ────────────────────────────────

    @Test
    void loginRequest9006_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(LoginRequest9006.class)).isTrue();
    }

    @Test
    void loginResponse9007_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(LoginResponse9007.class)).isTrue();
    }

    @Test
    void logoutRequest9008_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(LogoutRequest9008.class)).isTrue();
    }

    @Test
    void logoutResponse9009_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(LogoutResponse9009.class)).isTrue();
    }

    // ── LoginRequest9006 ───────────────────────────────────

    @Test
    void loginRequest9006_jaxbRoundtrip_shouldPreserveBothFields() throws Exception {
        LoginRequest9006 original = new LoginRequest9006();
        original.setPassword("p@ssw0rd");
        original.setNewPassword("n3wP@ssw0rd");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<LoginRequest9006")
                .contains("<Password>p@ssw0rd</Password>")
                .contains("<NewPassword>n3wP@ssw0rd</NewPassword>");

        LoginRequest9006 parsed = JaxbRoundtripSupport.unmarshal(xml, LoginRequest9006.class);
        assertThat(parsed.getPassword()).isEqualTo("p@ssw0rd");
        assertThat(parsed.getNewPassword()).isEqualTo("n3wP@ssw0rd");
    }

    @Test
    void loginRequest9006_optionalNewPassword_shouldBeOmittedWhenNull() throws Exception {
        LoginRequest9006 minimal = new LoginRequest9006();
        minimal.setPassword("p@ssw0rd");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .contains("<Password>p@ssw0rd</Password>")
                .doesNotContain("<NewPassword>");
    }

    @Test
    void loginRequest9006_toString_shouldMaskPasswords() {
        LoginRequest9006 req = new LoginRequest9006();
        req.setPassword("s3cret");
        req.setNewPassword("n3wP@ss");

        String output = req.toString();

        assertThat(output).isEqualTo("LoginRequest9006[Password=***,NewPassword=***]");
        assertThat(output).doesNotContain("s3cret");
        assertThat(output).doesNotContain("n3wP@ss");
    }

    @Test
    void loginRequest9006_toString_shouldReturnMaskedLiteralEvenWhenFieldsNull() {
        assertThat(new LoginRequest9006().toString())
                .isEqualTo("LoginRequest9006[Password=***,NewPassword=***]");
    }

    // ── LoginResponse9007 ──────────────────────────────────

    @Test
    void loginResponse9007_jaxbRoundtrip_shouldPreserveStatus() throws Exception {
        LoginResponse9007 original = new LoginResponse9007();
        original.setStatus("01");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<LoginResponse9007")
                .contains("<Status>01</Status>");

        LoginResponse9007 parsed = JaxbRoundtripSupport.unmarshal(xml, LoginResponse9007.class);
        assertThat(parsed.getStatus()).isEqualTo("01");
    }

    // ── LogoutRequest9008 ──────────────────────────────────

    @Test
    void logoutRequest9008_jaxbRoundtrip_shouldPreservePassword() throws Exception {
        LogoutRequest9008 original = new LogoutRequest9008();
        original.setPassword("p@ssw0rd");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<LogoutRequest9008")
                .contains("<Password>p@ssw0rd</Password>");

        LogoutRequest9008 parsed = JaxbRoundtripSupport.unmarshal(xml, LogoutRequest9008.class);
        assertThat(parsed.getPassword()).isEqualTo("p@ssw0rd");
    }

    @Test
    void logoutRequest9008_toString_shouldMaskPassword() {
        LogoutRequest9008 req = new LogoutRequest9008();
        req.setPassword("s3cret");

        String output = req.toString();

        assertThat(output).isEqualTo("LogoutRequest9008[Password=***]");
        assertThat(output).doesNotContain("s3cret");
    }

    @Test
    void logoutRequest9008_toString_shouldReturnMaskedLiteralEvenWhenFieldNull() {
        assertThat(new LogoutRequest9008().toString())
                .isEqualTo("LogoutRequest9008[Password=***]");
    }

    // ── LogoutResponse9009 ─────────────────────────────────

    @Test
    void logoutResponse9009_jaxbRoundtrip_shouldPreserveStatus() throws Exception {
        LogoutResponse9009 original = new LogoutResponse9009();
        original.setStatus("02");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<LogoutResponse9009")
                .contains("<Status>02</Status>");

        LogoutResponse9009 parsed = JaxbRoundtripSupport.unmarshal(xml, LogoutResponse9009.class);
        assertThat(parsed.getStatus()).isEqualTo("02");
    }

    // ── Forward9000 ────────────────────────────────────────

    @Test
    void forward9000_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(Forward9000.class)).isTrue();
    }

    @Test
    void forward9000_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        Forward9000 original = new Forward9000();
        original.setSrcNodeCode("A1000143000104");
        original.setSrcOrgCode("ORG-SRC-001");
        original.setDesNodeCode("A1000143000201");
        original.setDesOrgCode("ORG-DES-001");
        original.setBusinessNo("B001");
        original.setContent("<payload>hello</payload>");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<Forward9000")
                .contains("<SrcNodeCode>A1000143000104</SrcNodeCode>")
                .contains("<SrcOrgCode>ORG-SRC-001</SrcOrgCode>")
                .contains("<DesNodeCode>A1000143000201</DesNodeCode>")
                .contains("<DesOrgCode>ORG-DES-001</DesOrgCode>")
                .contains("<BusinessNo>B001</BusinessNo>")
                .contains("<Content>&lt;payload&gt;hello&lt;/payload&gt;</Content>");

        Forward9000 parsed = JaxbRoundtripSupport.unmarshal(xml, Forward9000.class);
        assertThat(parsed.getSrcNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getSrcOrgCode()).isEqualTo("ORG-SRC-001");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000201");
        assertThat(parsed.getDesOrgCode()).isEqualTo("ORG-DES-001");
        assertThat(parsed.getBusinessNo()).isEqualTo("B001");
        assertThat(parsed.getContent()).isEqualTo("<payload>hello</payload>");
    }

    @Test
    void forward9000_optionalBusinessNo_shouldBeOmittedWhenNull() throws Exception {
        Forward9000 minimal = new Forward9000();
        minimal.setSrcNodeCode("A1000143000104");
        minimal.setSrcOrgCode("ORG-SRC-001");
        minimal.setDesNodeCode("A1000143000201");
        minimal.setDesOrgCode("ORG-DES-001");
        minimal.setContent("raw-data");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .contains("<Content>raw-data</Content>")
                .doesNotContain("<BusinessNo>");
    }

    // ── Forward9100 ────────────────────────────────────────

    @Test
    void forward9100_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(Forward9100.class)).isTrue();
    }

    @Test
    void forward9100_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        Forward9100 original = new Forward9100();
        original.setSrcNodeCode("A1000143000104");
        original.setSrcOrgCode("ORG-SRC-100");
        original.setDesNodeCode("A1000143000201");
        original.setDesOrgCode("ORG-DES-100");
        original.setBusinessNo("B100");
        original.setContent("batch-payload");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<Forward9100")
                .contains("<SrcNodeCode>A1000143000104</SrcNodeCode>")
                .contains("<SrcOrgCode>ORG-SRC-100</SrcOrgCode>")
                .contains("<DesNodeCode>A1000143000201</DesNodeCode>")
                .contains("<DesOrgCode>ORG-DES-100</DesOrgCode>")
                .contains("<BusinessNo>B100</BusinessNo>")
                .contains("<Content>batch-payload</Content>");

        Forward9100 parsed = JaxbRoundtripSupport.unmarshal(xml, Forward9100.class);
        assertThat(parsed.getSrcNodeCode()).isEqualTo("A1000143000104");
        assertThat(parsed.getSrcOrgCode()).isEqualTo("ORG-SRC-100");
        assertThat(parsed.getDesNodeCode()).isEqualTo("A1000143000201");
        assertThat(parsed.getDesOrgCode()).isEqualTo("ORG-DES-100");
        assertThat(parsed.getBusinessNo()).isEqualTo("B100");
        assertThat(parsed.getContent()).isEqualTo("batch-payload");
    }

    @Test
    void forward9100_optionalBusinessNo_shouldBeOmittedWhenNull() throws Exception {
        Forward9100 minimal = new Forward9100();
        minimal.setSrcNodeCode("A1000143000104");
        minimal.setSrcOrgCode("ORG-SRC-100");
        minimal.setDesNodeCode("A1000143000201");
        minimal.setDesOrgCode("ORG-DES-100");
        minimal.setContent("batch-payload");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .contains("<Content>batch-payload</Content>")
                .doesNotContain("<BusinessNo>");
    }

    // ── MsgReturn9020 ──────────────────────────────────────

    @Test
    void msgReturn9020_shouldExtendCfxBody() {
        assertThat(CfxBody.class.isAssignableFrom(MsgReturn9020.class)).isTrue();
    }

    @Test
    void msgReturn9020_jaxbRoundtrip_shouldPreserveAllFields() throws Exception {
        MsgReturn9020 original = new MsgReturn9020();
        original.setOriMsgNo("9000");
        original.setDebug("trace-id=abc-123");

        String xml = JaxbRoundtripSupport.marshal(original);

        assertThat(xml)
                .contains("<MsgReturn9020")
                .contains("<OriMsgNo>9000</OriMsgNo>")
                .contains("<Debug>trace-id=abc-123</Debug>");

        MsgReturn9020 parsed = JaxbRoundtripSupport.unmarshal(xml, MsgReturn9020.class);
        assertThat(parsed.getOriMsgNo()).isEqualTo("9000");
        assertThat(parsed.getDebug()).isEqualTo("trace-id=abc-123");
    }

    @Test
    void msgReturn9020_optionalDebug_shouldBeOmittedWhenNull() throws Exception {
        MsgReturn9020 minimal = new MsgReturn9020();
        minimal.setOriMsgNo("9000");

        String xml = JaxbRoundtripSupport.marshal(minimal);

        assertThat(xml)
                .contains("<OriMsgNo>9000</OriMsgNo>")
                .doesNotContain("<Debug>");
    }
}
