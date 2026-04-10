package com.puchain.fep.converter.xml;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.converter.model.CfxMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * CFX 报文 JAXB 编解码器。参见 PRD v1.3 §3.2.1。
 *
 * <p>线程安全：{@link JAXBContext} 本身线程安全，在构造器中一次性初始化；
 * Marshaller/Unmarshaller 非线程安全，每次调用新建（符合 JAXB 规范）。</p>
 *
 * <p><b>严格校验约束</b>：本类是 fep-converter 模块的唯一 JAXB 入口。
 * 所有通过 {@link #unmarshal(String)} 的入站报文会被注册一个严格的
 * {@code ValidationEventHandler}（{@code event -> false}），目的是让
 * {@link com.puchain.fep.converter.model.CommonHead} 等模型类 setter 里抛出的
 * {@link IllegalArgumentException} 正确升级为
 * {@link jakarta.xml.bind.UnmarshalException}，最终被本类包装为
 * {@link MessageConverterException} with {@link FepErrorCode#CONV_8002}。</p>
 *
 * <p>这项约束源于 jaxb-runtime 4.x (Eclipse) 默认 ValidationEventHandler 行为：
 * 非 FATAL_ERROR 级别事件被默认 handler 静默吞掉，unmarshal 继续，字段留空。
 * 见 {@link com.puchain.fep.converter.model.CommonHead} 类级 Javadoc。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class XmlCodec {

    /** 标准 UTF-8 XML 声明（不含 standalone 属性）。 */
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    /** 典型 CFX 报文 XML 大小 ~1KB，预分配 2KB 避免扩容。 */
    private static final int INITIAL_WRITER_CAPACITY = 2048;

    private final JAXBContext context;

    /**
     * 构造 XmlCodec，初始化 JAXBContext。
     *
     * @throws IllegalStateException 如果 JAXBContext 初始化失败（通常是注解冲突）
     */
    public XmlCodec() {
        try {
            this.context = JAXBContext.newInstance(CfxMessage.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialize JAXBContext for CfxMessage", e);
        }
    }

    /**
     * 序列化 CfxMessage 为 UTF-8 XML 字符串。
     *
     * @param message 待序列化的 CfxMessage
     * @return XML 字符串（以 {@code <?xml version="1.0" encoding="UTF-8"?>} 开头）
     * @throws MessageConverterException CONV_8001 如果序列化失败
     */
    public String marshal(final CfxMessage message) {
        try {
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            // JAXB_FRAGMENT=true 抑制 jaxb-runtime 默认输出的
            // {@code standalone="yes"} 属性；由本类显式前置标准 UTF-8 声明。
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter sw = new StringWriter(INITIAL_WRITER_CAPACITY);
            sw.write(XML_DECLARATION);
            m.marshal(message, sw);
            return sw.toString();
        } catch (JAXBException e) {
            throw new MessageConverterException(FepErrorCode.CONV_8001, "marshal failed", e);
        }
    }

    /**
     * 反序列化 XML 字符串为 CfxMessage。安装严格 ValidationEventHandler，
     * 使 setter 校验异常升级为 UnmarshalException。
     *
     * @param xml XML 字符串
     * @return 反序列化后的 CfxMessage
     * @throws MessageConverterException CONV_8002 如果反序列化失败
     *         （XML 格式错误或 setter 校验失败）
     */
    public CfxMessage unmarshal(final String xml) {
        try {
            Unmarshaller u = context.createUnmarshaller();
            // jaxb-runtime 4.x 默认 handler 会吞 ERROR 事件，安装严格 handler
            // 让 setter 抛的 IllegalArgumentException 升级为 UnmarshalException
            u.setEventHandler(event -> false);
            return (CfxMessage) u.unmarshal(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (JAXBException e) {
            throw new MessageConverterException(FepErrorCode.CONV_8002, "unmarshal failed", e);
        }
    }
}
