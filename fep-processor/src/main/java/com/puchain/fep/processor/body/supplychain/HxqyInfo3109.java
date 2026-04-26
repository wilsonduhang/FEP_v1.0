package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * Local complexType {@code hxqyInfo} (3109 variant) — 核心企业登记信息段（2 字段）。
 *
 * <p>字段顺序严格对应 {@code 3109.xsd} 中 {@code hxqyInfo} complexType 的 sequence:
 * hxqyNum, hxqyInfoMx[1..20]。两字段均为 required。</p>
 *
 * <p><b>区别于 {@link HxqyInfo}（3107/3112 共享）</b>：本类与 T4 的 {@link HxqyInfo}
 * 在 XSD 中**同名 {@code hxqyInfo} 但字段集合完全不同**：
 * <ul>
 *   <li>3107/3112 的 {@code hxqyInfo} = (hxqyName, hxqyCode) — 核心企业基本信息</li>
 *   <li>3109 的 {@code hxqyInfo} = (hxqyNum, hxqyInfoMx[1..20]) — wrapper 段，承载多个明细</li>
 * </ul>
 * 两 complexType 同名但字段语义、结构均无交集，必须独立成类。本类带 {@code 3109} 后缀
 * 以避免与 {@link HxqyInfo} 命名冲突；T4 类不带后缀以匹配 3107/3112 间真正的 complexType
 * 共享事实。</p>
 *
 * <p><b>JAXB 隔离</b>：{@code @XmlType(name="hxqyInfo")} 与 {@link HxqyInfo} 的同名 XSD type
 * 不会冲突，因每条 3109 报文只走 {@link QyRegister3109} 单类 JAXBContext，
 * 生产时（{@link com.puchain.fep.processor.pipeline.BatchMessageProcessorService})
 * 也按 body class 分别 cache JAXBContext，不会与 3107/3112 共上下文。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "hxqyInfo")
@XmlType(name = "hxqyInfo", propOrder = {
        "hxqyNum", "hxqyInfoMx"
})
public class HxqyInfo3109 extends CfxBody {

    @XmlElement(name = "hxqyNum", required = true)
    private String hxqyNum;

    @XmlElement(name = "hxqyInfoMx", required = true)
    private List<HxqyInfoMx> hxqyInfoMx;

    /**
     * Returns the count of core enterprise details (核心企业明细条数).
     *
     * @return count
     */
    public String getHxqyNum() {
        return hxqyNum;
    }

    /**
     * Sets the count of core enterprise details.
     *
     * @param v count
     */
    public void setHxqyNum(final String v) {
        this.hxqyNum = v;
    }

    /**
     * Returns the core enterprise detail list (核心企业明细, 1-20 entries).
     *
     * @return detail list
     */
    public List<HxqyInfoMx> getHxqyInfoMx() {
        return hxqyInfoMx;
    }

    /**
     * Sets the core enterprise detail list.
     *
     * @param v detail list
     */
    public void setHxqyInfoMx(final List<HxqyInfoMx> v) {
        this.hxqyInfoMx = v;
    }
}
