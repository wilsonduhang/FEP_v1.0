package com.puchain.fep.processor.body.common;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 9008 节点登出请求 Body POJO（PRD v1.3 §4.5 + §3.7 节点工作流程）。
 *
 * <p>Fields follow {@code 9008.xsd} {@code LogoutRequest9008} complexType sequence:
 * {@code Password} (required).</p>
 *
 * <p><b>Security (v1b)</b>:
 * <ul>
 *   <li>{@link #toString()} masks {@code Password} ({@code ***}); 不泄漏明文到日志。</li>
 *   <li>JAXB {@code Marshaller.marshal(req, ...)} 输出的 XML byte[] 中
 *       {@code <Password>明文</Password>} 是 HNDEMP XSD 合约必需的明文。
 *       业务代码 <b>禁止</b>直接
 *       {@code log.info("xml: {}", new String(xml, UTF_8))}；
 *       排障需打印 POJO（{@code log.info("req: {}", req)} 走 {@link #toString()}
 *       脱敏）或使用 P3 将引入的 XML 脱敏过滤器。</li>
 * </ul></p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "LogoutRequest9008")
@XmlType(propOrder = {"password"})
public class LogoutRequest9008 extends CfxBody {

    @XmlElement(name = "Password", required = true)
    private String password;

    /**
     * @return 节点登录密码（敏感字段，避免直接打印；使用 {@link #toString()} 获得脱敏字符串）
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置节点登录密码。
     *
     * @param v 节点登录密码
     */
    public void setPassword(final String v) {
        this.password = v;
    }

    /**
     * 脱敏字符串表示。{@code Password} 始终输出 {@code ***}，不泄漏明文。
     *
     * @return 脱敏字符串
     */
    @Override
    public String toString() {
        return "LogoutRequest9008[Password=***]";
    }
}
