package com.puchain.fep.converter.pipeline;

/**
 * 报文编解码流水线选项。
 *
 * <p>控制 sign / zip / encrypt 三个可选阶段的开关及所需密钥。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class MessagePipelineOptions {

    private boolean sign = true;
    private boolean zip;
    private boolean encrypt;
    private byte[] signPrivateKey;
    private byte[] signPublicKey;
    private byte[] encryptKey;

    /**
     * @return 是否启用签名
     */
    public boolean isSign() {
        return sign;
    }

    /**
     * @param v 是否启用签名
     */
    public void setSign(final boolean v) {
        this.sign = v;
    }

    /**
     * @return 是否启用压缩
     */
    public boolean isZip() {
        return zip;
    }

    /**
     * @param v 是否启用压缩
     */
    public void setZip(final boolean v) {
        this.zip = v;
    }

    /**
     * @return 是否启用加密
     */
    public boolean isEncrypt() {
        return encrypt;
    }

    /**
     * @param v 是否启用加密
     */
    public void setEncrypt(final boolean v) {
        this.encrypt = v;
    }

    /**
     * @return SM2 签名私钥
     */
    public byte[] getSignPrivateKey() {
        return signPrivateKey;
    }

    /**
     * @param v SM2 签名私钥
     */
    public void setSignPrivateKey(final byte[] v) {
        this.signPrivateKey = v;
    }

    /**
     * @return SM2 验签公钥
     */
    public byte[] getSignPublicKey() {
        return signPublicKey;
    }

    /**
     * @param v SM2 验签公钥
     */
    public void setSignPublicKey(final byte[] v) {
        this.signPublicKey = v;
    }

    /**
     * @return SM4 加密密钥
     */
    public byte[] getEncryptKey() {
        return encryptKey;
    }

    /**
     * @param v SM4 加密密钥
     */
    public void setEncryptKey(final byte[] v) {
        this.encryptKey = v;
    }
}
