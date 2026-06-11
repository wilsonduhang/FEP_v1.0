package com.puchain.fep.security.impl.hash;

import com.puchain.fep.security.api.HashService;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;

/**
 * SM3 摘要实现（BouncyCastle lightweight API，GB/T 32905-2016）。
 *
 * <p>无 Spring stereotype，经 {@code GmHashConfiguration @Bean} 注册
 * （红线 feedback_provider_switch_impl_no_stereotype_bean_registration）。
 * lightweight {@link SM3Digest} 不依赖 JCA provider 注册，线程安全由每次调用新建 digest 保证。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class HashServiceImpl implements HashService {

    @Override
    public String sm3Hex(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        final SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        final byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return Hex.toHexString(out);
    }
}
