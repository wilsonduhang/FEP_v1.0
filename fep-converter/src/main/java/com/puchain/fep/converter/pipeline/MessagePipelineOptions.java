package com.puchain.fep.converter.pipeline;

/**
 * 报文编解码流水线选项。
 *
 * <p>控制 sign / zip / encrypt 三个可选阶段的开关。GM S2b（形态 C-ev）起，签名私钥/验签公钥
 * 不再经本选项穿参——加签私钥由 {@code MessageSignPort} 经 KeyService 单源取，验签公钥按
 * {@link #getSrcNode()} 路由（PRD §3.3.3 步骤 1）。本选项仅保留解码验签所需的 srcNode 路由键。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class MessagePipelineOptions {

    private boolean sign = true;
    private boolean zip;
    private boolean encrypt;
    private String srcNode;
    private byte[] encryptKey;

    /** 默认无参构造器。 */
    public MessagePipelineOptions() {
        // default values applied via field initializers
    }

    /**
     * 复制构造器，用于无副作用地派生变体。
     *
     * <p>{@code encryptKey} byte[] 为浅拷贝（共享引用）——由调用方密钥生命周期与
     * security-api 层统一管理，此处不做防御性复制；srcNode 为不可变 String。</p>
     *
     * @param other 要复制的源对象
     */
    public MessagePipelineOptions(final MessagePipelineOptions other) {
        this.sign = other.sign;
        this.zip = other.zip;
        this.encrypt = other.encrypt;
        this.srcNode = other.srcNode;
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
     * @return 入站验签发起方节点代码（SrcNode，PRD §3.3.3 步骤 1 公钥路由键）
     */
    public String getSrcNode() {
        return srcNode;
    }

    /**
     * @param v 入站验签发起方节点代码（SrcNode）
     */
    public void setSrcNode(final String v) {
        this.srcNode = v;
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
