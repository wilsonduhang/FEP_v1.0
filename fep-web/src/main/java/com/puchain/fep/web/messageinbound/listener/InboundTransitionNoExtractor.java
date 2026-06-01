package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Extracts the authoritative {@code TransitionNo} from an inbound CFX payload's
 * business head (BatchHead/RealHead) element.
 *
 * <p>R3 transitionNo 派生规范化升级：PRD §3.2.3/§3.2.4 业务头携带真实
 * {@code TransitionNo}（8 位流水号）。本类用 XXE-hardened DOM + XPath
 * {@code /CFX/MSG/*}{@code /TransitionNo} 直接读取业务头文本值，替代
 * {@code TlqInboundListener} 历史占位（msgId 末 8 位伪派生）。</p>
 *
 * <p>与 {@code InboundMessageDispatcher} P3 Task 5 finding 一致：业务头
 * POJO（{@code RequestBusinessHead} 等）无 {@code @XmlRootElement}，在
 * {@code @XmlAnyElement(lax=true)} 下解析为 DOM Element 不可 cast，故不走
 * JAXB POJO 路径而用 XPath 直读 raw XML。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
final class InboundTransitionNoExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(InboundTransitionNoExtractor.class);

    /** 业务头 TransitionNo 文本：CFX/MSG 任一直接孙元素（仅 BatchHead/RealHead 含此元素）。 */
    private static final String TRANSITION_NO_XPATH = "/CFX/MSG/*/TransitionNo/text()";

    /**
     * XXE-hardened factory built once at class-load. A parser that cannot apply
     * the required hardening features fails fast here (loud {@link IllegalStateException}
     * at startup) rather than silently degrading every extraction to the msgId
     * fallback at runtime — see MAJOR-1 review finding.
     */
    private static final DocumentBuilderFactory DBF = createHardenedFactory();

    private InboundTransitionNoExtractor() {
        // utility class — no instances
    }

    /**
     * Extract the business-head {@code TransitionNo} from a CFX payload.
     *
     * @param payloadXml the raw CFX XML string, may be {@code null}
     * @return trimmed TransitionNo, or {@link Optional#empty()} when absent,
     *         blank, or the payload cannot be parsed safely
     */
    // CRLF: e.getMessage() wrapped by LogSanitizer.sanitize. XXE: factory hardened
    // in createHardenedFactory (disallow-doctype-decl + secure-processing + external
    // entities disabled), runtime-proven by extract_xxePayload_rejectedReturnsEmpty.
    @SuppressFBWarnings(value = {"CRLF_INJECTION_LOGS", "XXE_DOCUMENT"},
            justification = "CRLF: LogSanitizer.sanitize wraps message, find-sec-bugs "
                    + "cannot detect user-defined sanitizer; XXE: DocumentBuilderFactory "
                    + "hardened in createHardenedFactory, find-sec-bugs cannot trace "
                    + "static factory feature setup")
    static Optional<String> extract(final String payloadXml) {
        if (payloadXml == null || payloadXml.isBlank()) {
            return Optional.empty();
        }
        try {
            // Parse-time errors (malformed XML, DOCTYPE rejected by hardening,
            // XPath eval) are legitimate "no real value" cases → caller falls back.
            // Note: parser CONFIG errors already failed fast at class-load (DBF init),
            // so reaching here means the parser itself is healthy.
            final Document doc = DBF.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(
                            payloadXml.getBytes(StandardCharsets.UTF_8)));
            // XPathFactory is NOT thread-safe per JAXP spec — create per call (onMessage
            // may run concurrently on a real TLQ broker; SPI lookup cost is negligible
            // at inbound rate). Supersedes the static-cache micro-opt (final review MINOR-2).
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final String value = (String) xpath.evaluate(
                    TRANSITION_NO_XPATH, doc, XPathConstants.STRING);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            // trim() is defensive: text() normally carries no boundary whitespace, but a
            // pretty-printed/mixed-content TransitionNo element could; isBlank already
            // guaranteed non-empty content above.
            return Optional.of(value.trim());
        } catch (ParserConfigurationException | org.xml.sax.SAXException
                | java.io.IOException | javax.xml.xpath.XPathExpressionException e) {
            LOG.debug("TransitionNo extract failed, will fall back to derived: {}",
                    LogSanitizer.sanitize(e.getMessage()));
            return Optional.empty();
        }
    }

    private static DocumentBuilderFactory createHardenedFactory() {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // XXE hardening — inbound payload is external, untrusted.
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (ParserConfigurationException e) {
            // fail-fast: a parser that cannot be hardened must NOT silently process
            // untrusted XML (and must not silently degrade every extraction to fallback).
            throw new IllegalStateException(
                    "XML parser does not support required XXE-hardening features", e);
        }
        dbf.setExpandEntityReferences(false);
        dbf.setXIncludeAware(false);
        dbf.setNamespaceAware(false);
        return dbf;
    }
}
