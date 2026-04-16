package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * 便捷工厂方法：构造带 HEAD 和多个 Body 子元素的报文。
     *
     * @param head   报文头
     * @param bodies Body 对象（0 个、1 个或多个均可）
     * @return CfxMessage 实例
     */
    public static CfxMessage of(final CommonHead head, final Object... bodies) {
        CfxMessage m = new CfxMessage();
        m.head = head;
        m.msgContainer = new MsgContainer();
        if (bodies != null) {
            for (Object body : bodies) {
                if (body != null) {
                    m.msgContainer.getContents().add(body);
                }
            }
        }
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
     * JAXB 绑定点。业务代码请使用 {@link #getBody()} 或 {@link #getBodies()}。
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
     * 业务侧便捷访问：返回 MSG 内的第一个 Body 对象。
     * 如果 MSG 包含多个子元素，请使用 {@link #getBodies()}。
     *
     * @return 第一个 Body 对象，可能为 null
     */
    public Object getBody() {
        if (msgContainer == null) {
            return null;
        }
        List<Object> contents = msgContainer.getContents();
        return contents.isEmpty() ? null : contents.get(0);
    }

    /**
     * 返回 MSG 内的所有 Body 子元素。
     *
     * @return 不可变的 Body 列表，不为 null
     */
    public List<Object> getBodies() {
        if (msgContainer == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(msgContainer.getContents());
    }

    /**
     * {@code <MSG>} 容器。使用 {@code @XmlAnyElement(lax=true)} 在 {@code List} 上
     * 支持 0 个、1 个或多个子元素：
     * <ul>
     *   <li>注册到 JAXBContext 的具体 Body POJO → 反序列化为对应类型</li>
     *   <li>未注册类型 → 反序列化为 {@code org.w3c.dom.Element}</li>
     * </ul>
     *
     * <p><b>P1b-DEFECT-001 修复</b>：原实现使用单个 {@code Object content}，
     * 当 {@code <MSG>} 包含 2 个子元素时仅保留最后一个。改为 {@code List<Object>}
     * 后 JAXB 自动收集全部子元素。</p>
     */
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class MsgContainer {

        private List<Object> contents = new ArrayList<>();

        /**
         * 获取 Body 内容列表。
         *
         * @return Body 内容列表（可变，JAXB unmarshal 会直接 add）
         */
        @XmlAnyElement(lax = true)
        public List<Object> getContents() {
            return contents;
        }

        /**
         * 设置 Body 内容列表。
         *
         * @param contents Body 内容列表
         */
        public void setContents(final List<Object> contents) {
            this.contents = contents != null ? contents : new ArrayList<>();
        }
    }
}
