package com.puchain.fep.converter.pipeline;

import com.puchain.fep.converter.model.CfxMessage;

/**
 * 解码流水线输出：还原的 CfxMessage + 验签结果。
 *
 * <p>{@code verified=false} 时，业务层可根据场景决定拒绝或放行
 * （例如连通性测试类报文可宽松处理）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class DecodeResult {

    private final CfxMessage message;
    private final boolean verified;

    /**
     * 构造解码结果。
     *
     * @param message 还原的 CfxMessage
     * @param verified 验签是否通过
     */
    public DecodeResult(final CfxMessage message, final boolean verified) {
        this.message = message;
        this.verified = verified;
    }

    /**
     * @return 还原的 CfxMessage
     */
    public CfxMessage getMessage() {
        return message;
    }

    /**
     * @return 验签是否通过，{@code false} 表示无签名或签名错误
     */
    public boolean isVerified() {
        return verified;
    }
}
