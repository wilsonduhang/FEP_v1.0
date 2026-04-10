package com.puchain.fep.converter.model;

/**
 * CFX 报文体抽象基类标记。
 *
 * <p>具体的 44 种报文 Body POJO 由 P3 processor 模块按各自 XSD 定义并继承本类。
 * 本 Plan 仅建立类型标记，{@link CfxMessage} 通过 {@code @XmlAnyElement(lax=true)}
 * 接纳任意 Body 类型（含未注册类型的 DOM Element 回退）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public abstract class CfxBody {
}
