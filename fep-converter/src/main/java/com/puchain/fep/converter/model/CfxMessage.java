package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * CFX 报文根元素。参见 PRD v1.3 §3.2.1。
 *
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <CFX>
 *     <HEAD>...</HEAD>
 *     <MSG>...</MSG>
 * </CFX>
 * <!--Base64 签名-->
 * }</pre>
 *
 * <p><b>设计说明</b>：JAXB 不允许同一字段同时标注
 * {@code @XmlElement} 和 {@code @XmlAnyElement}。因此 {@code <MSG>}
 * 使用内部 {@link MsgContainer} 包装类：外层 {@code @XmlElement}
 * 固定元素名，内层 {@code @XmlAnyElement(lax=true)} 接纳 44 种具体
 * Body POJO（由 P3 processor 注册）或 {@code org.w3c.dom.Element} 回退。</p>
 *
 * <p><b>访问策略</b>：使用 {@code @XmlAccessorType(XmlAccessType.PROPERTY)}
 * 让 JAXB unmarshal 走 getter/setter 路径，入站伪造/损坏报文在 setter
 * 校验阶段即被拒绝。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlRootElement(name = "CFX")
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {"head", "msgContainer"})
public class CfxMessage {

    private CommonHead head;
    private MsgContainer msgContainer = new MsgContainer();

    /**
     * 默认构造器（JAXB 需要）。
     */
    public CfxMessage() {
        // JAXB
    }

    /**
     * 便捷工厂方法：构造带 HEAD 和 Body 的报文。
     *
     * @param head 报文头
     * @param body Body 对象（可为 null）
     * @return CfxMessage 实例
     */
    public static CfxMessage of(final CommonHead head, final Object body) {
        CfxMessage m = new CfxMessage();
        m.head = head;
        m.msgContainer = new MsgContainer();
        m.msgContainer.setContent(body);
        return m;
    }

    /**
     * 获取报文头。
     *
     * @return 报文头
     */
    @XmlElement(name = "HEAD", required = true)
    public CommonHead getHead() {
        return head;
    }

    /**
     * 设置报文头。
     *
     * @param head 报文头
     */
    public void setHead(final CommonHead head) {
        this.head = head;
    }

    /**
     * JAXB 绑定点。业务代码请使用 {@link #getBody()}。
     *
     * @return MSG 容器
     */
    @XmlElement(name = "MSG", required = true)
    public MsgContainer getMsgContainer() {
        return msgContainer;
    }

    /**
     * 设置 MSG 容器。
     *
     * @param msgContainer MSG 容器
     */
    public void setMsgContainer(final MsgContainer msgContainer) {
        this.msgContainer = msgContainer;
    }

    /**
     * 业务侧便捷访问：返回 MSG 内的实际 Body 对象（或 DOM Element）。
     *
     * @return Body 对象，可能为 null
     */
    public Object getBody() {
        return msgContainer == null ? null : msgContainer.getContent();
    }

    /**
     * {@code <MSG>} 容器。使用 {@code @XmlAnyElement(lax=true)} 支持：
     * <ul>
     *   <li>注册到 JAXBContext 的具体 Body POJO → 反序列化为对应类型</li>
     *   <li>未注册类型 → 反序列化为 {@code org.w3c.dom.Element}</li>
     * </ul>
     */
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class MsgContainer {

        private Object content;

        /**
         * 获取 Body 内容。
         *
         * @return Body 内容
         */
        @XmlAnyElement(lax = true)
        public Object getContent() {
            return content;
        }

        /**
         * 设置 Body 内容。
         *
         * @param content Body 内容
         */
        public void setContent(final Object content) {
            this.content = content;
        }
    }
}
