package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OutboundHeadXmlParser} (B1 SpotBugs permanent fix Task 1).
 *
 * <p>Coverage:</p>
 * <ul>
 *     <li>Missing {@code sendOrgCode} → {@link FepErrorCode#OUTBOUND_5106_HEAD_FIELDS_INVALID}</li>
 *     <li>Missing {@code entrustDate} → {@link FepErrorCode#OUTBOUND_5106_HEAD_FIELDS_INVALID}</li>
 *     <li>Missing {@code transitionNo} → {@link FepErrorCode#OUTBOUND_5106_HEAD_FIELDS_INVALID}</li>
 *     <li>Happy path: all fields present</li>
 *     <li>Root element name mismatch (v1.1 F1 fix coverage)</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OutboundHeadXmlParserTest {

    @Test
    void parse_shouldThrowMissingFieldException_whenSendOrgCodeMissing() {
        String xml = "<OutboundHeadFields>"
                + "<entrustDate>20260507</entrustDate>"
                + "<transitionNo>00000001</transitionNo>"
                + "</OutboundHeadFields>";
        assertThatThrownBy(() -> OutboundHeadXmlParser.parse(xml))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("sendOrgCode")
                .extracting("errorCode")
                .isEqualTo(FepErrorCode.OUTBOUND_5106_HEAD_FIELDS_INVALID);
    }

    @Test
    void parse_shouldThrowMissingFieldException_whenEntrustDateMissing() {
        String xml = "<OutboundHeadFields>"
                + "<sendOrgCode>BANK001</sendOrgCode>"
                + "<transitionNo>00000001</transitionNo>"
                + "</OutboundHeadFields>";
        assertThatThrownBy(() -> OutboundHeadXmlParser.parse(xml))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("entrustDate");
    }

    @Test
    void parse_shouldThrowMissingFieldException_whenTransitionNoMissing() {
        String xml = "<OutboundHeadFields>"
                + "<sendOrgCode>BANK001</sendOrgCode>"
                + "<entrustDate>20260507</entrustDate>"
                + "</OutboundHeadFields>";
        assertThatThrownBy(() -> OutboundHeadXmlParser.parse(xml))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("transitionNo");
    }

    @Test
    void parse_shouldSucceed_whenAllFieldsPresent() {
        String xml = "<OutboundHeadFields>"
                + "<sendOrgCode>BANK001</sendOrgCode>"
                + "<entrustDate>20260507</entrustDate>"
                + "<transitionNo>00000001</transitionNo>"
                + "</OutboundHeadFields>";
        var fields = OutboundHeadXmlParser.parse(xml);
        assertThat(fields.sendOrgCode()).isEqualTo("BANK001");
        assertThat(fields.entrustDate()).isEqualTo("20260507");
        assertThat(fields.transitionNo()).isEqualTo("00000001");
    }

    @Test
    void parse_shouldThrowException_whenRootElementNameMismatch() {
        String xml = "<NotOutboundHeadFields>"
                + "<sendOrgCode>BANK001</sendOrgCode>"
                + "</NotOutboundHeadFields>";
        assertThatThrownBy(() -> OutboundHeadXmlParser.parse(xml))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FepErrorCode.OUTBOUND_5106_HEAD_FIELDS_INVALID);
    }

    @Test
    void parse_shouldThrowNullPointerException_whenXmlIsNull() {
        assertThatThrownBy(() -> OutboundHeadXmlParser.parse(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("xml");
    }
}
