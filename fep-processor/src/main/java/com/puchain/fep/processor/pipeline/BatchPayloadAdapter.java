package com.puchain.fep.processor.pipeline;

import com.puchain.fep.transport.support.PayloadSplitter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 批量报文 Body 分拆适配（PRD §3.1 TLQ 8KB 约束）。
 *
 * <p>薄封装 {@link PayloadSplitter}（{@code fep-transport.support}），
 * 对上层暴露 {@link #needsSplit(String)} + {@link #split(String)}
 * 两个方法，上层 {@link BatchMessageProcessorService} 无需直接依赖 transport
 * 层的 {@code SplitResult} 以外的内部细节。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BatchPayloadAdapter {

    /** 单 TLQ 消息属性的 UTF-8 字节上限（对齐 {@link PayloadSplitter#MAX_PART_BYTES}）。 */
    private static final int MAX_BYTES_PER_SEGMENT = 8 * 1024;

    /**
     * 判断 payload 是否需要分拆（UTF-8 字节长度 &gt; 8KB）。
     *
     * @param payload XML 串；{@code null} 返回 {@code false}
     * @return {@code true} 当且仅当字节长度超过 8KB
     */
    public boolean needsSplit(final String payload) {
        if (payload == null) {
            return false;
        }
        return payload.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_PER_SEGMENT;
    }

    /**
     * 分拆 payload 为 {@link PayloadSplitter.SplitResult}（xmlstr/xmlstr1/xmlstr2）。
     *
     * @param payload XML 串，非空
     * @return 分拆结果（xmlstr 始终非空，xmlstr1/xmlstr2 按需）
     */
    public PayloadSplitter.SplitResult split(final String payload) {
        return PayloadSplitter.split(payload);
    }
}
