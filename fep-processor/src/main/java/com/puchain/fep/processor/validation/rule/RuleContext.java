package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.processor.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 报文字段的只读解析视图，供业务校验规则按 local-name 读取字段值。
 *
 * <p>命名空间无关：所有元素按本地名（local-name）索引，与 CFX envelope 多命名空间策略一致。
 * 同名元素（明细列表）按文档顺序保留全部文本值。</p>
 *
 * <p>解析时启用 XXE 防护（禁用 DOCTYPE / 外部实体），与 {@code XsdSchemaRegistry} 守护一致。</p>
 */
public final class RuleContext {

    private final Map<String, List<String>> fields;

    private RuleContext(final Map<String, List<String>> fields) {
        this.fields = fields;
    }

    /**
     * 解析 UTF-8 XML 字节为只读字段视图。
     *
     * @param xml UTF-8 编码报文字节，非空
     * @return 字段视图
     * @throws ValidationException XML 非良构或解析失败
     */
    public static RuleContext parse(final byte[] xml) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // Deny external DTD/schema access — align with sibling XsdValidator /
            // XsdSchemaRegistry XXE posture (FindSecBugs XXE_DOCUMENT keys on these).
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            dbf.setExpandEntityReferences(false);
            final DocumentBuilder builder = dbf.newDocumentBuilder();
            final Document doc = builder.parse(new ByteArrayInputStream(xml));
            final Map<String, List<String>> collected = new HashMap<>();
            collect(doc.getDocumentElement(), collected);
            return new RuleContext(collected);
        } catch (final ValidationException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new ValidationException("rule context parse failed: " + ex.getMessage(), ex);
        }
    }

    private static void collect(final Node node, final Map<String, List<String>> out) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }
        final Element el = (Element) node;
        final String localName = el.getLocalName() != null ? el.getLocalName() : el.getTagName();
        final String text = directTextOf(el);
        if (text != null && !text.isBlank()) {
            out.computeIfAbsent(localName, k -> new ArrayList<>()).add(text.trim());
        }
        final NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collect(children.item(i), out);
        }
    }

    private static String directTextOf(final Element el) {
        final NodeList children = el.getChildNodes();
        final StringBuilder sb = new StringBuilder();
        boolean hasText = false;
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
                hasText = true;
            }
        }
        return hasText ? sb.toString() : null;
    }

    /**
     * 返回指定 local-name 字段的全部文本值（文档顺序）。
     *
     * @param localName 元素本地名
     * @return 不可修改的值列表；字段不存在时为空 List
     */
    public List<String> values(final String localName) {
        return List.copyOf(fields.getOrDefault(localName, List.of()));
    }

    /**
     * 返回指定 local-name 字段的首个文本值。
     *
     * @param localName 元素本地名
     * @return 首个值；字段不存在时 {@link Optional#empty()}
     */
    public Optional<String> first(final String localName) {
        final List<String> vs = fields.get(localName);
        return (vs == null || vs.isEmpty()) ? Optional.empty() : Optional.of(vs.get(0));
    }

    /**
     * 字段是否存在且有非空白值。
     *
     * @param localName 元素本地名
     * @return 存在且非空白返回 true
     */
    public boolean has(final String localName) {
        return first(localName).isPresent();
    }
}
