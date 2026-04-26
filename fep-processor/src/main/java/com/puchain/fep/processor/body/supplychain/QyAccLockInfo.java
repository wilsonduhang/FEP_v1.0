package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.List;

/**
 * Local complexType {@code qyAccLockInfo} — 回款锁定企业登记信息段（2 字段）。
 *
 * <p>字段顺序严格对应 {@code 3109.xsd} 中 {@code qyAccLockInfo} complexType 的 sequence:
 * qyAccLockNum, qyAccLockInfoMx[1..20]。两字段均为 required。</p>
 *
 * <p>本类作为 {@link QyRegister3109#getQyAccLockInfo()} 的可选嵌套块出现
 * （3109.xsd {@code minOccurs="0"}），承载多个回款锁定企业明细。</p>
 *
 * <p>所有标量字段 Java 类型统一为 {@link String}；XSD 长度/格式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "qyAccLockInfo")
@XmlType(name = "qyAccLockInfo", propOrder = {
        "qyAccLockNum", "qyAccLockInfoMx"
})
public class QyAccLockInfo extends CfxBody {

    @XmlElement(name = "qyAccLockNum", required = true)
    private String qyAccLockNum;

    @XmlElement(name = "qyAccLockInfoMx", required = true)
    private List<QyAccLockInfoMx> qyAccLockInfoMx;

    /**
     * Returns the count of payback-locked enterprise details (回款锁定企业明细条数).
     *
     * @return count
     */
    public String getQyAccLockNum() {
        return qyAccLockNum;
    }

    /**
     * Sets the count of payback-locked enterprise details.
     *
     * @param v count
     */
    public void setQyAccLockNum(final String v) {
        this.qyAccLockNum = v;
    }

    /**
     * Returns the payback-locked enterprise detail list (回款锁定企业明细, 1-20 entries).
     *
     * @return detail list
     */
    public List<QyAccLockInfoMx> getQyAccLockInfoMx() {
        return qyAccLockInfoMx;
    }

    /**
     * Sets the payback-locked enterprise detail list.
     *
     * @param v detail list
     */
    public void setQyAccLockInfoMx(final List<QyAccLockInfoMx> v) {
        this.qyAccLockInfoMx = v;
    }
}
