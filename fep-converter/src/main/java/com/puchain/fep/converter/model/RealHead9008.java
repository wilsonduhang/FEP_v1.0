package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 9008 节点登出请求业务头 POJO（PRD v1.3 §3.2.2 + §4.5）。
 *
 * <p>结构与字段约束见父类 {@link AbstractRealHead}；本类仅声明
 * {@code @XmlRootElement(name="RealHead9008")} 区分根元素名。</p>
 *
 * <p>v1c 创建（P2c 遗留缺口，P1c T7 build9008Message 依赖此类）。
 * P1c T10 R1 closing 抽公共父类（与 {@link RealHead9006} 共享 3 字段 + 校验）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlRootElement(name = "RealHead9008")
public class RealHead9008 extends AbstractRealHead {
}
