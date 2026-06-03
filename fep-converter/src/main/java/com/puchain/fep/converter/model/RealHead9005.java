package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 9005 节点心跳请求业务头 POJO（PRD v1.3 §3.2.2 + §4.5）。
 *
 * <p>结构与字段约束见父类 {@link AbstractRealHead}；本类仅声明
 * {@code @XmlRootElement(name="RealHead9005")} 区分根元素名（head-only 心跳报文无 body）。</p>
 *
 * <p>P4-MSG-O 创建（{@code TlqNodeLoginService.build9005Message} 依赖；与 {@link RealHead9006}/{@link RealHead9008}
 * 共享 3 字段父类 {@link AbstractRealHead}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlRootElement(name = "RealHead9005")
public class RealHead9005 extends AbstractRealHead {
}
