package com.puchain.fep.converter.compress;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * ZIP (Deflate) + Base64 报文压缩/解压工具。
 *
 * <p>对应 PRD v1.3 §3.1.3，当 TLQ 消息属性 {@code zip} 为 true 时，报文文本需先经
 * Deflate 压缩再 Base64 编码后放入 xmlstr 传输；接收端按相反顺序还原。</p>
 *
 * <p>当同时启用加密时，按 PRD §3.4.2 的要求，顺序必须是 <strong>先压缩再加密</strong>。</p>
 *
 * <p>本类仅使用 JDK 原生 {@link java.util.zip.Deflater} / {@link java.util.zip.Inflater}
 * 与 {@link java.util.Base64}，不引入任何第三方依赖。Native 资源通过 {@code finally}
 * 中的 {@code end()} 强制释放，防止内存泄漏。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class ZipBase64Compressor {

    /**
     * Deflate / Inflate 循环读写缓冲区大小（字节）。
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * 解压输出缓冲区初始容量倍数。Deflate 压缩比一般 2~5 倍，
     * 4 倍作为启发式初值避免 ByteArrayOutputStream 多次扩容。
     */
    private static final int DECOMPRESS_CAPACITY_MULTIPLIER = 4;

    /**
     * 对输入字符串执行 Deflate 压缩后 Base64 编码。
     *
     * @param input 待压缩的 UTF-8 字符串，不得为 {@code null}
     * @return Base64 编码后的压缩字符串
     * @throws MessageConverterException 压缩失败时抛出 {@link FepErrorCode#CONV_8005}
     */
    public String compress(final String input) {
        final byte[] src = input.getBytes(StandardCharsets.UTF_8);
        final Deflater deflater = new Deflater();
        deflater.setInput(src);
        deflater.finish();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(src.length)) {
            final byte[] buf = new byte[BUFFER_SIZE];
            while (!deflater.finished()) {
                final int n = deflater.deflate(buf);
                out.write(buf, 0, n);
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new MessageConverterException(FepErrorCode.CONV_8005, "compress failed", e);
        } finally {
            deflater.end();
        }
    }

    /**
     * 对 Base64 字符串执行解码后 Inflate 解压。
     *
     * @param base64 Base64 编码的压缩字符串，不得为 {@code null}
     * @return 解压后的 UTF-8 字符串
     * @throws MessageConverterException Base64 非法或 Deflate 数据损坏时抛出
     *                                   {@link FepErrorCode#CONV_8005}
     */
    public String decompress(final String base64) {
        final byte[] compressed;
        try {
            compressed = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new MessageConverterException(FepErrorCode.CONV_8005, "invalid base64", e);
        }
        final Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        try (ByteArrayOutputStream out =
                     new ByteArrayOutputStream(compressed.length * DECOMPRESS_CAPACITY_MULTIPLIER)) {
            final byte[] buf = new byte[BUFFER_SIZE];
            while (!inflater.finished()) {
                final int n = inflater.inflate(buf);
                if (n == 0) {
                    // 单次 setInput 已消费完毕（空流或流末尾），退出循环
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                }
                out.write(buf, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (DataFormatException | IOException e) {
            throw new MessageConverterException(FepErrorCode.CONV_8005, "decompress failed", e);
        } finally {
            inflater.end();
        }
    }
}
