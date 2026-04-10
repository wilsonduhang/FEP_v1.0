package com.puchain.fep.processor.validation;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads HNDEMP message XSDs under {@code fep-processor/resources/xsd/} and caches
 * them keyed by {@link MessageType}.
 *
 * <p>Lazy-loading with {@link ConcurrentHashMap}. XSDs reference shared type
 * definitions via {@code <xsd:include schemaLocation="./Base.xsd"/>}, so a
 * classpath-based {@link LSResourceResolver} is registered.</p>
 *
 * <p>P2a scope supports 11 synchronous messages (1001/1004/2001/2004/3001-3006/9005);
 * all other {@link MessageType} values throw {@link UnsupportedOperationException}.</p>
 *
 * <p>Thread safety: the {@link SchemaFactory} is created once in the constructor and
 * assigned to a {@code final} field, guaranteeing safe publication under the JMM.
 * {@link Schema} instances returned by {@code newSchema} are thread-safe per JAXP.</p>
 */
@Component
public class XsdSchemaRegistry {

    private static final Logger log = LoggerFactory.getLogger(XsdSchemaRegistry.class);

    private static final Set<String> SUPPORTED_CODES = Set.of(
            "1001", "1004", "2001", "2004",
            "3001", "3002", "3003", "3004", "3005", "3006",
            "9005"
    );

    private static final String XSD_CLASSPATH_DIR = "/xsd/";

    private final Map<String, Schema> cache = new ConcurrentHashMap<>();

    private final SchemaFactory factory;

    /**
     * Creates the registry and eagerly initializes the {@link SchemaFactory} with
     * XXE protection ({@link XMLConstants#FEATURE_SECURE_PROCESSING}) and a
     * classpath-based {@link LSResourceResolver}. Schemas themselves are still
     * loaded lazily via {@link #schemaOf(MessageType)}.
     */
    public XsdSchemaRegistry() {
        this.factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (SAXException e) {
            throw new FepBusinessException(FepErrorCode.PROC_8503,
                    "Failed to enable secure XML processing", e);
        }
        factory.setResourceResolver(new ClasspathXsdResolver());
    }

    /**
     * Returns the cached {@link Schema} for the given message type. Thread-safe;
     * the schema is parsed on first access and cached for subsequent calls.
     *
     * @param type the message type
     * @return non-null {@link Schema}
     * @throws UnsupportedOperationException if {@code type} is outside the P2a scope
     * @throws FepBusinessException if the XSD resource cannot be loaded or parsed
     */
    public Schema schemaOf(final MessageType type) {
        String code = type.msgNo();
        if (!SUPPORTED_CODES.contains(code)) {
            throw new UnsupportedOperationException(
                    "MessageType " + code + " is not supported in P2a (sync mode). "
                            + "Supported: " + SUPPORTED_CODES);
        }
        return cache.computeIfAbsent(code, this::loadSchema);
    }

    private Schema loadSchema(final String code) {
        String path = XSD_CLASSPATH_DIR + code + ".xsd";
        try (InputStream is = XsdSchemaRegistry.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new FepBusinessException(FepErrorCode.PROC_8503,
                        "XSD resource not found: " + path);
            }
            Schema schema = factory.newSchema(new StreamSource(is, path));
            log.info("Loaded XSD schema for message type {}", code);
            return schema;
        } catch (SAXException | java.io.IOException e) {
            throw new FepBusinessException(FepErrorCode.PROC_8503,
                    "Failed to load XSD for " + code, e);
        }
    }

    /**
     * Resolves {@code <xsd:include>} references (e.g. {@code ./Base.xsd} or
     * {@code ./DataType.xsd}) from the classpath {@code /xsd/} directory.
     */
    private static final class ClasspathXsdResolver implements LSResourceResolver {

        @Override
        public LSInput resolveResource(final String type,
                                       final String namespaceURI,
                                       final String publicId,
                                       final String systemId,
                                       final String baseURI) {
            if (systemId == null) {
                return null;
            }
            final String resourceName = systemId.startsWith("./") ? systemId.substring(2) : systemId;
            final InputStream resourceStream = XsdSchemaRegistry.class
                    .getResourceAsStream(XSD_CLASSPATH_DIR + resourceName);
            if (resourceStream == null) {
                return null;
            }
            return new ClasspathLSInput(resourceStream, systemId, publicId, baseURI);
        }
    }

    /**
     * Minimal {@link LSInput} backed by a classpath {@link InputStream}, used by
     * {@link ClasspathXsdResolver} to feed XSD includes into the JAXP parser.
     */
    private static final class ClasspathLSInput implements LSInput {
        private InputStream byteStream;
        private String systemId;
        private String publicId;
        private String baseURI;

        ClasspathLSInput(final InputStream byteStream,
                         final String systemId,
                         final String publicId,
                         final String baseURI) {
            this.byteStream = byteStream;
            this.systemId = systemId;
            this.publicId = publicId;
            this.baseURI = baseURI;
        }

        @Override
        public InputStream getByteStream() {
            return byteStream;
        }

        @Override
        public void setByteStream(final InputStream b) {
            this.byteStream = b;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(final Reader r) {
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(final String s) {
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(final String s) {
            this.systemId = s;
        }

        @Override
        public String getPublicId() {
            return publicId;
        }

        @Override
        public void setPublicId(final String p) {
            this.publicId = p;
        }

        @Override
        public String getBaseURI() {
            return baseURI;
        }

        @Override
        public void setBaseURI(final String b) {
            this.baseURI = b;
        }

        @Override
        public String getEncoding() {
            return "UTF-8";
        }

        @Override
        public void setEncoding(final String e) {
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(final boolean c) {
        }
    }
}
