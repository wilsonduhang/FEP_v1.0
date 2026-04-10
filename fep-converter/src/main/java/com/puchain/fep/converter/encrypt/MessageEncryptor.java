package com.puchain.fep.converter.encrypt;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import com.puchain.fep.security.api.CryptoService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 报文 SM4 加解密编排。参见 PRD v1.3 §3.4.2 应用加密传输。
 *
 * <p>流程：</p>
 * <ol>
 *   <li>{@link #encrypt}: UTF-8 字节 → {@link CryptoService#encrypt} → Base64 编码</li>
 *   <li>{@link #decrypt}: Base64 解码 → {@link CryptoService#decrypt} → UTF-8 字符串</li>
 * </ol>
 *
 * <p>如同时启用压缩（PRD §3.4.2 "如同时压缩+加密，应先压缩再加密，最后 Base64"），
 * 调用顺序由上层 {@code MessageEncoder}（Task 10）编排，本类不做顺序约束。</p>
 *
 * <p>⛔ 本类不直接执行 SM4 算法，所有加密原语通过
 * {@code com.puchain.fep.security.api.CryptoService} 调用，实现由 security-mock
 * （开发期）或 security-impl（生产期，人工编写）提供。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class MessageEncryptor {

    private final CryptoService cryptoService;

    /**
     * 构造注入 {@link CryptoService}。
     *
     * @param cryptoService 加解密服务实现
     */
    public MessageEncryptor(final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    /**
     * 加密并 Base64 编码。
     *
     * @param plaintext UTF-8 明文字符串
     * @param key SM4 密钥字节
     * @return Base64 编码后的密文字符串
     * @throws MessageConverterException CONV_8006 如果加密失败
     */
    public String encrypt(final String plaintext, final byte[] key) {
        try {
            final byte[] cipher = cryptoService.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), key);
            return Base64.getEncoder().encodeToString(cipher);
        } catch (RuntimeException e) {
            throw new MessageConverterException(FepErrorCode.CONV_8006, "encrypt failed", e);
        }
    }

    /**
     * Base64 解码后解密为 UTF-8 字符串。
     *
     * @param base64Ciphertext Base64 编码的密文字符串
     * @param key SM4 密钥字节
     * @return 解密后的 UTF-8 明文
     * @throws MessageConverterException CONV_8006 如果 Base64 非法或解密失败
     */
    public String decrypt(final String base64Ciphertext, final byte[] key) {
        final byte[] cipher;
        try {
            cipher = Base64.getDecoder().decode(base64Ciphertext);
        } catch (IllegalArgumentException e) {
            throw new MessageConverterException(FepErrorCode.CONV_8006, "invalid base64", e);
        }
        try {
            final byte[] plain = cryptoService.decrypt(cipher, key);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw new MessageConverterException(FepErrorCode.CONV_8006, "decrypt failed", e);
        }
    }
}
