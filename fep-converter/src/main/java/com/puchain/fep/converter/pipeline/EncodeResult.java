package com.puchain.fep.converter.pipeline;

/**
 * 编码流水线输出：最终 payload 字符串 + zip/encrypt 标志（供 TLQ 属性设置）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class EncodeResult {

    private final String payload;
    private final boolean zip;
    private final boolean encrypt;

    /**
     * 构造编码结果。
     *
     * @param payload 最终 payload 字符串
     * @param zip 是否经过压缩
     * @param encrypt 是否经过加密
     */
    public EncodeResult(final String payload, final boolean zip, final boolean encrypt) {
        this.payload = payload;
        this.zip = zip;
        this.encrypt = encrypt;
    }

    /**
     * @return payload 字符串
     */
    public String getPayload() {
        return payload;
    }

    /**
     * @return 是否经过压缩
     */
    public boolean isZip() {
        return zip;
    }

    /**
     * @return 是否经过加密
     */
    public boolean isEncrypt() {
        return encrypt;
    }
}
