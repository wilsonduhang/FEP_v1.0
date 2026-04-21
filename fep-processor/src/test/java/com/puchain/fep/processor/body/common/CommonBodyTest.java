package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.processor.body.JaxbRoundtripSupport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JAXB marshal/unmarshal roundtrip tests + toString masking assertions for
 * common Body POJOs (9006/9007/9008/9009 node login/logout).
 *
 * <p>Covers PRD v1.3 §4.5 node workflow messages:
 * <ul>
 *   <li>{@link LoginRequest9006} — Password / NewPassword (both masked in toString).</li>
 *   <li>{@link LoginResponse9007} — Status (non-sensitive).</li>
 *   <li>{@link LogoutRequest9008} — Password (masked in toString).</li>
 *   <li>{@link LogoutResponse9009} — Status (non-sensitive).</li>
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
}
