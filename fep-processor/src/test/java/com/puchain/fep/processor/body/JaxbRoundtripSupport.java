package com.puchain.fep.processor.body;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * 测试工具：对任意 {@code @XmlRootElement} POJO 做 JAXB marshal/unmarshal 往返。
 *
 * <p>仅在 fep-processor 测试作用域使用，用于 Body POJO（1001/2001/1004/2004 等）的
 * 独立 XML 序列化验证；marshal 采用 {@code JAXB_FRAGMENT=true}，输出 XML 片段不含 {@code <?xml ... ?>} 声明。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class JaxbRoundtripSupport {

    private JaxbRoundtripSupport() {
    }

    /**
     * 将给定 JAXB 注解实例 marshal 为 XML 片段（{@code JAXB_FORMATTED_OUTPUT=true}，
     * {@code JAXB_FRAGMENT=true}，无 XML 声明）。
     *
     * @param instance 要序列化的 JAXB POJO，必须带 {@link jakarta.xml.bind.annotation.XmlRootElement}
     * @param <T>      实例类型
     * @return 格式化的 XML 字符串
     * @throws Exception JAXB 上下文创建或 marshal 过程中的任何异常
     */
    public static <T> String marshal(final T instance) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(instance.getClass());
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter writer = new StringWriter();
        marshaller.marshal(instance, writer);
        return writer.toString();
    }

    /**
     * 将 XML 字符串 unmarshal 为指定类型的 JAXB POJO 实例。
     *
     * @param xml  XML 输入字符串
     * @param type 目标 JAXB 注解类型
     * @param <T>  目标类型
     * @return 反序列化的 POJO 实例
     * @throws Exception JAXB 上下文创建或 unmarshal 过程中的任何异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(final String xml, final Class<T> type) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(type);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        return (T) unmarshaller.unmarshal(new StringReader(xml));
    }
}
