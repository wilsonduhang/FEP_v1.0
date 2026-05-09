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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Loads HNDEMP message XSDs under {@code fep-processor/resources/xsd/} and caches
 * them keyed by {@link MessageType}.
 *
 * <p>Eagerly loads all 41 supported XSDs at construction time using a single-threaded
 * {@link SchemaFactory}, then discards the factory. The resulting {@link Schema}
 * instances are thread-safe per JAXP and cached in an unmodifiable map.</p>
 *
 * <p>Current scope supports 44 supported messages
 * (1001/1004/1101/1102/1103/1104/2001/2004/2101/2102/2103/2104/3000/3001-3009/3020/3101/3102/3103/3105/
 * 3107/3108/3109/3112/3113/3115/3116/3120/9000/9005/9006-9009/9020/9100/9120);
 * all other {@link MessageType} values throw {@link UnsupportedOperationException}.</p>
 */
@Component
public class XsdSchemaRegistry {

    private static final Logger log = LoggerFactory.getLogger(XsdSchemaRegistry.class);

    private static final Set<String> SUPPORTED_CODES = Set.of(
            "1001", "1004", "1101", "1102", "1103", "1104", "2001", "2004", "2101", "2102", "2103", "2104",
            "3000", "3001", "3002", "3003", "3004", "3005", "3006", "3007", "3008", "3009",
            "3020", "3101", "3102", "3103", "3105", "3107", "3108", "3109",
            "3112", "3113", "3115", "3116", "3120",
            "9000", "9005", "9006", "9007", "9008", "9009",
            "9020", "9100", "9120"
    );

    private static final String XSD_CLASSPATH_DIR = "/xsd/";

    private final Map<String, Schema> cache;

    /**
     * Creates the registry, eagerly loading all 41 supported XSDs into an
     * unmodifiable cache. The {@link SchemaFactory} is used only during
     * construction (single-threaded), avoiding its documented thread-safety
     * limitations.
     */
    public XsdSchemaRegistry() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException e) {
            throw new FepBusinessException(FepErrorCode.PROC_8503,
                    "Failed to enable secure XML processing", e);
        }
        factory.setResourceResolver(new ClasspathXsdResolver());

        Map<String, Schema> loaded = new HashMap<>(SUPPORTED_CODES.size());
        for (String code : SUPPORTED_CODES) {
            loaded.put(code, loadSchema(factory, code));
        }
        this.cache = Map.copyOf(loaded);
    }

    /**
     * Returns the pre-loaded {@link Schema} for the given message type.
     *
     * @param type the message type
     * @return non-null {@link Schema}
     * @throws UnsupportedOperationException if {@code type} is outside the P2a/P2b/P2c/P2d scope
     */
    public Schema schemaOf(final MessageType type) {
        String code = type.msgNo();
        Schema schema = cache.get(code);
        if (schema == null) {
            throw new UnsupportedOperationException(
                    "MessageType " + code + " is not supported. "
                            + "Supported: " + SUPPORTED_CODES);
        }
        return schema;
    }

    private static Schema loadSchema(final SchemaFactory factory, final String code) {
        String path = XSD_CLASSPATH_DIR + code + ".xsd";
        try (InputStream is = XsdSchemaRegistry.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new FepBusinessException(FepErrorCode.PROC_8503,
                        "XSD resource not found: " + path);
            }
            // FindSecBugs XXE_SCHEMA_FACTORY: re-assert at call site for
            // intra-procedural analysis (idempotent with constructor hardening).
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
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
    static final class ClasspathXsdResolver implements LSResourceResolver {

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
    static final class ClasspathLSInput implements LSInput {
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
