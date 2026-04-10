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

    /** 默认无参构造器。 */
    public MessagePipelineOptions() {
        // default values applied via field initializers
    }

    /**
     * 复制构造器，用于无副作用地派生变体。
     *
     * <p>byte[] 密钥字段为浅拷贝（共享引用）——它们由调用方的密钥生命周期
     * 与 security-api 层统一管理，此处不做防御性复制。</p>
     *
     * @param other 要复制的源对象
     */
    public MessagePipelineOptions(final MessagePipelineOptions other) {
        this.sign = other.sign;
        this.zip = other.zip;
        this.encrypt = other.encrypt;
        this.signPrivateKey = other.signPrivateKey;
        this.signPublicKey = other.signPublicKey;
        this.encryptKey = other.encryptKey;
    }

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
