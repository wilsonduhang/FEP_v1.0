package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 9007 节点登录回执 Body POJO（PRD v1.3 §4.5 + §3.7 节点工作流程）。
 *
 * <p>Fields follow {@code 9007.xsd} {@code LoginResponse9007} complexType sequence:
 * {@code Status} (required, {@code NodeStatus} type).</p>
 *
 * <p><b>Security (v1b)</b>:
 * <ul>
 *   <li>{@code Status} 字段本身非敏感，采用默认 {@link Object#toString()} 实现。</li>
 *   <li>但 9007 是 9006 登录请求的回执，与 {@link LoginRequest9006} 同属节点鉴权流程；
 *       业务代码 <b>禁止</b>直接打印 JAXB marshal 输出的 XML byte[]
 *       ({@code log.info("xml: {}", new String(xml, UTF_8))})，
 *       避免在同一日志流中混入上游 9006 的 Password 明文。排障请打印 POJO
 *       或使用 P3 将引入的 XML 脱敏过滤器。</li>
 * </ul></p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "LoginResponse9007")
@XmlType(propOrder = {"status"})
public class LoginResponse9007 extends CfxBody implements SerialNoBearing {

    @XmlElement(name = "Status", required = true)
    private String status;

    /**
     * @return 节点当前状态（{@code NodeStatus} 枚举值）
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置节点当前状态。
     *
     * @param v 节点状态
     */
    public void setStatus(final String v) {
        this.status = v;
    }

    /**
     * 9007 节点登录回执无业务 SerialNo 字段（仅 Status），实现
     * {@link SerialNoBearing} 仅为满足 {@code InboundMessageDispatcher}
     * 注册表 ArchUnit 不变量（与 2101/2102/2103/2104 BATCH 回执同策略）。
     * 永远返回 {@code null} → dispatcher fallback 到 transitionNo。
     *
     * @return 恒为 {@code null}
     */
    @Override
    public String getSerialNo() {
        return null;
    }
}
