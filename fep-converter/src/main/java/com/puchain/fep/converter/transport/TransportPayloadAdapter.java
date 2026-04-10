package com.puchain.fep.converter.transport;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.converter.pipeline.DecodeResult;
import com.puchain.fep.converter.pipeline.EncodeResult;
import com.puchain.fep.converter.pipeline.MessageDecoder;
import com.puchain.fep.converter.pipeline.MessagePipelineOptions;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 将 {@link com.puchain.fep.converter.pipeline.MessageEncoder} 的编码输出
 * 封装为 TLQ 通信层可投递的 {@link TlqMessage}，以及反向解包。
 *
 * <p>职责边界：仅做“容器装配”——根据 {@link TlqChannel#isRealtime()} 选择
 * 实时/非实时属性策略，透传 {@link EncodeResult#isZip()} / {@link EncodeResult#isEncrypt()}
 * 到 {@link TlqMessageAttributes}，并对 &gt;24KB 单消息直接拒绝（应走文件通道）。</p>
 *
 * <p>&gt;24KB 拒绝原因：TLQ 单消息最大 xmlstr + xmlstr1 + xmlstr2 = 24KB（PRD §3.1.3）。
 * 超出者应由 P3 processor 在业务层决定走文件通道（.sm4 加密文件 + §3.5 命名规范），
 * 或重构报文。P1a PayloadSplitter 仅负责 ≤24KB 下的分片传输。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class TransportPayloadAdapter {

    /** TLQ 单消息最大 24KB（xmlstr + xmlstr1 + xmlstr2 合计上限）。 */
    public static final int MAX_PAYLOAD_BYTES = 24 * 1024;

    private final MessageDecoder decoder;

    /**
     * 构造适配器。
     *
     * @param decoder 入站解码流水线
     */
    public TransportPayloadAdapter(final MessageDecoder decoder) {
        this.decoder = decoder;
    }

    /**
     * 将 {@link EncodeResult} 封装为 {@link TlqMessage}。
     *
     * <p>实时通道使用 {@link TlqMessageAttributes#forRealtime(String)}（非持久化、30s 过期），
     * 非实时通道使用 {@link TlqMessageAttributes#forBatch(String)}（持久化、不过期）。
     * zip / encrypt 标志透传自 {@link EncodeResult}。</p>
     *
     * @param result 编码结果
     * @param channel 目标通道
     * @param msgId 20 位报文 ID
     * @return 可投递的 TlqMessage
     * @throws MessageConverterException {@link FepErrorCode#CONV_8007} 当 payload UTF-8 字节数 &gt; 24KB
     */
    public TlqMessage toTlqMessage(final EncodeResult result,
                                   final TlqChannel channel,
                                   final String msgId) {
        final byte[] payloadBytes = result.getPayload().getBytes(StandardCharsets.UTF_8);
        if (payloadBytes.length > MAX_PAYLOAD_BYTES) {
            throw new MessageConverterException(FepErrorCode.CONV_8007,
                    "payload size " + payloadBytes.length
                            + " bytes exceeds 24KB TLQ single-message limit; "
                            + "must use file channel (.sm4) per PRD §3.5");
        }
        final TlqMessageAttributes attrs = channel.isRealtime()
                ? TlqMessageAttributes.forRealtime(msgId)
                : TlqMessageAttributes.forBatch(msgId);
        attrs.setZip(result.isZip());
        attrs.setEncrypt(result.isEncrypt());
        return new TlqMessage(result.getPayload(), attrs, channel);
    }

    /**
     * 从 {@link TlqMessage} 反向解码。
     *
     * <p>始终以 {@link TlqMessageAttributes} 的 zip / encrypt 为准覆盖调用方 opts，
     * 防止调用方传入的 opts 与实际线上属性不一致导致解码失败。</p>
     *
     * @param message 入站 TLQ 消息
     * @param opts 流水线选项（zip / encrypt 会被强制回填）
     * @return 解码结果
     */
    public DecodeResult fromTlqMessage(final TlqMessage message, final MessagePipelineOptions opts) {
        opts.setZip(message.getAttributes().isZip());
        opts.setEncrypt(message.getAttributes().isEncrypt());
        return decoder.decode(message.getPayload(), opts);
    }
}
