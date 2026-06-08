package com.puchain.fep.security.impl.crypto;

import com.puchain.fep.security.api.CryptoService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * SM4/ECB/PKCS7Padding 真实加解密实现（BouncyCastle "BC" provider，GB/T 32907-2016）。
 *
 * <p>BC GM provider 由 {@link BouncyCastleGmProviderConfig}（S0）启动时幂等注册。
 * 严格遵 PRD §3.4.2（ECB + PKCS#7），与 HNDEMP 互通。密钥由 {@code KeyService} 提供，
 * 本类不持有任何密钥材料（无状态单例）。</p>
 *
 * <p><strong>🔓 Mode A:</strong> 2026-06-07 muzhou 解禁国密域，本实现 AI 编写 + 密码学
 * 专项 review（GB/T 32907 标准向量逐字节验证）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class CryptoServiceImpl implements CryptoService {

    /** SM4 算法变换字符串（ECB + PKCS#7 填充）。 */
    private static final String SM4_TRANSFORMATION = "SM4/ECB/PKCS7Padding";

    /** SM4 密钥算法名。 */
    private static final String SM4_ALGORITHM = "SM4";

    /** BouncyCastle provider 名（S0 注册）。 */
    private static final String BC_PROVIDER = "BC";

    /** SM4 密钥长度（字节）。 */
    private static final int SM4_KEY_LENGTH = 16;

    @Override
    public byte[] encrypt(final byte[] plaintext, final byte[] key) {
        validate(plaintext, key);
        return doCipher(Cipher.ENCRYPT_MODE, plaintext, key);
    }

    @Override
    public byte[] decrypt(final byte[] ciphertext, final byte[] key) {
        validate(ciphertext, key);
        return doCipher(Cipher.DECRYPT_MODE, ciphertext, key);
    }

    private static void validate(final byte[] data, final byte[] key) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (key.length != SM4_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "SM4 key length must be " + SM4_KEY_LENGTH + " bytes, got " + key.length);
        }
    }

    @SuppressFBWarnings(value = "CIPHER_INTEGRITY",
            justification = "SM4/ECB/PKCS7 is mandated by PRD §3.4.2 + GB/T 32907-2016 for HNDEMP "
                    + "interop (not a free choice). Message integrity/authenticity is provided at a "
                    + "separate layer by the SM3withSM2 signature on the CFX envelope, not by the "
                    + "symmetric cipher mode.")
    private static byte[] doCipher(final int mode, final byte[] data, final byte[] key) {
        try {
            final Cipher cipher = Cipher.getInstance(SM4_TRANSFORMATION, BC_PROVIDER);
            cipher.init(mode, new SecretKeySpec(key, SM4_ALGORITHM));
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SM4 " + (mode == Cipher.ENCRYPT_MODE
                    ? "encryption" : "decryption") + " failed", e);
        }
    }
}
