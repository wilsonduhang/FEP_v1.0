package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.model.CfxBody;
import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.processor.body.common.ExtInfo;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 3002 业务进展查询回执报文业务体。
 *
 * <p>字段顺序严格对应 {@code 3002.xsd} 中 {@code ProgressQueryReturn3002} complexType 的 sequence：
 * SerialNo, SendNodeCode, DesNodeCode, hxqyName, hxqyCode, QueryType, QueryKey,
 * ReturnCode, ReturnMemo?, ExtInfo?。</p>
 *
 * <p>所有文本字段 Java 类型统一为 {@link String}；{@link ExtInfo} 为可选嵌套扩展块。
 * XSD 层面的长度/格式/模式约束由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ProgressQueryReturn3002")
@XmlType(propOrder = {
        "serialNo", "sendNodeCode", "desNodeCode",
        "hxqyName", "hxqyCode",
        "queryType", "queryKey",
        "returnCode", "returnMemo", "extInfo"
})
public class ProgressQueryReturn3002 extends CfxBody implements SerialNoBearing {

    @XmlElement(name = "SerialNo", required = true)
    private String serialNo;

    @XmlElement(name = "SendNodeCode", required = true)
    private String sendNodeCode;

    @XmlElement(name = "DesNodeCode", required = true)
    private String desNodeCode;

    @XmlElement(name = "hxqyName", required = true)
    private String hxqyName;

    @XmlElement(name = "hxqyCode", required = true)
    private String hxqyCode;

    @XmlElement(name = "QueryType", required = true)
    private String queryType;

    @XmlElement(name = "QueryKey", required = true)
    private String queryKey;

    @XmlElement(name = "ReturnCode", required = true)
    private String returnCode;

    @XmlElement(name = "ReturnMemo")
    private String returnMemo;

    @XmlElement(name = "ExtInfo")
    private ExtInfo extInfo;

    /**
     * Returns the serial number.
     *
     * @return serial number
     */
    public String getSerialNo() {
        return serialNo;
    }

    /**
     * Sets the serial number.
     *
     * @param v serial number
     */
    public void setSerialNo(final String v) {
        this.serialNo = v;
    }

    /**
     * Returns the sending node code (14-char).
     *
     * @return sending node code
     */
    public String getSendNodeCode() {
        return sendNodeCode;
    }

    /**
     * Sets the sending node code.
     *
     * @param v sending node code
     */
    public void setSendNodeCode(final String v) {
        this.sendNodeCode = v;
    }

    /**
     * Returns the destination node code (14-char).
     *
     * @return destination node code
     */
    public String getDesNodeCode() {
        return desNodeCode;
    }

    /**
     * Sets the destination node code.
     *
     * @param v destination node code
     */
    public void setDesNodeCode(final String v) {
        this.desNodeCode = v;
    }

    /**
     * Returns the core enterprise name.
     *
     * @return core enterprise name
     */
    public String getHxqyName() {
        return hxqyName;
    }

    /**
     * Sets the core enterprise name.
     *
     * @param v core enterprise name
     */
    public void setHxqyName(final String v) {
        this.hxqyName = v;
    }

    /**
     * Returns the core enterprise USCI code (18-char).
     *
     * @return core enterprise USCI code
     */
    public String getHxqyCode() {
        return hxqyCode;
    }

    /**
     * Sets the core enterprise USCI code.
     *
     * @param v core enterprise USCI code
     */
    public void setHxqyCode(final String v) {
        this.hxqyCode = v;
    }

    /**
     * Returns the query type code.
     *
     * @return query type code
     */
    public String getQueryType() {
        return queryType;
    }

    /**
     * Sets the query type code.
     *
     * @param v query type code
     */
    public void setQueryType(final String v) {
        this.queryType = v;
    }

    /**
     * Returns the query key.
     *
     * @return query key
     */
    public String getQueryKey() {
        return queryKey;
    }

    /**
     * Sets the query key.
     *
     * @param v query key
     */
    public void setQueryKey(final String v) {
        this.queryKey = v;
    }

    /**
     * Returns the return code.
     *
     * @return return code
     */
    public String getReturnCode() {
        return returnCode;
    }

    /**
     * Sets the return code.
     *
     * @param v return code
     */
    public void setReturnCode(final String v) {
        this.returnCode = v;
    }

    /**
     * Returns the optional return memo/description.
     *
     * @return return memo, or {@code null} if absent
     */
    public String getReturnMemo() {
        return returnMemo;
    }

    /**
     * Sets the optional return memo/description.
     *
     * @param v return memo
     */
    public void setReturnMemo(final String v) {
        this.returnMemo = v;
    }

    /**
     * Returns the optional extension info block.
     *
     * @return extension info, or {@code null} if absent
     */
    public ExtInfo getExtInfo() {
        return extInfo;
    }

    /**
     * Sets the optional extension info block.
     *
     * @param v extension info
     */
    public void setExtInfo(final ExtInfo v) {
        this.extInfo = v;
    }
}
