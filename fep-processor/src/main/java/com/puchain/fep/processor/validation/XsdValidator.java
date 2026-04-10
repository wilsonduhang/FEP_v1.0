package com.puchain.fep.processor.validation;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 {@link javax.xml.validation.Validator} 的 XSD 结构校验器。
 * 仅做字段级结构/格式/长度校验，不涉及业务语义。
 */
@Component
public class XsdValidator {

    private static final Logger log = LoggerFactory.getLogger(XsdValidator.class);

    private static final int MAX_XML_SIZE = 10 * 1024 * 1024;

    private final XsdSchemaRegistry registry;

    /**
     * 构造 XsdValidator。
     *
     * @param registry XSD schema 注册表（非 null）
     */
    public XsdValidator(final XsdSchemaRegistry registry) {
        this.registry = registry;
    }

    /**
     * 对指定报文类型的 XML payload 做 XSD 结构校验。
     *
     * @param type 报文类型（用于查找对应的 schema）
     * @param xml  XML payload 字节数组（非 null，≤ 10MB）
     * @return 校验结果；valid=true 表示通过
     * @throws IllegalArgumentException xml 为 null
     * @throws ValidationException      xml 超过 10MB 或 IO 错误
     */
    public ValidationResult validate(final MessageType type, final byte[] xml) {
        if (xml == null) {
            throw new IllegalArgumentException("xml must not be null");
        }
        if (xml.length > MAX_XML_SIZE) {
            throw new ValidationException(
                    "XML payload size " + xml.length + " exceeds max " + MAX_XML_SIZE);
        }

        Schema schema = registry.schemaOf(type);
        Validator validator = schema.newValidator();
        // XXE hardening on the Validator instance itself — the Schema parse-time
        // hardening in XsdSchemaRegistry protects schema loading, but a fresh
        // Validator must also deny external DTD/schema access at validation time
        // (FindSecBugs XXE_VALIDATOR).
        try {
            validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException e) {
            throw new ValidationException("Failed to harden Validator against XXE", e);
        }
        CollectingErrorHandler handler = new CollectingErrorHandler();
        validator.setErrorHandler(handler);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(xml)) {
            validator.validate(new StreamSource(bis));
        } catch (SAXException e) {
            handler.errors.add(formatError(e));
        } catch (IOException e) {
            throw new ValidationException("IO error while validating XML", e);
        }

        String msgNoLabel = LogSanitizer.sanitize(type.msgNo());
        if (handler.errors.isEmpty()) {
            log.debug("XSD validation passed for {}", msgNoLabel);
            return ValidationResult.ok();
        }
        log.info("XSD validation failed for {}: {} error(s) - first: {}",
                msgNoLabel,
                handler.errors.size(),
                LogSanitizer.sanitize(handler.errors.get(0)));
        return ValidationResult.failed(handler.errors);
    }

    private static String formatError(final SAXException e) {
        if (e instanceof SAXParseException pe) {
            return "line " + pe.getLineNumber() + " col " + pe.getColumnNumber() + ": "
                    + pe.getMessage();
        }
        return e.getMessage();
    }

    private static final class CollectingErrorHandler implements ErrorHandler {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void warning(final SAXParseException exception) {
            // 忽略 warning
        }

        @Override
        public void error(final SAXParseException exception) {
            errors.add(formatError(exception));
        }

        @Override
        public void fatalError(final SAXParseException exception) {
            errors.add("fatal: " + formatError(exception));
        }
    }
}
