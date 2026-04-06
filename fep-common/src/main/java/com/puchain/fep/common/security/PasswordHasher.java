package com.puchain.fep.common.security;

/**
 * 密码散列服务接口。
 *
 * <p>开发期使用 BCrypt 实现，生产期可替换为国密 SM3 实现。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface PasswordHasher {

    /**
     * 生成密码散列值。
     *
     * @param rawPassword 明文密码，不可为 null 或空
     * @return 散列值字符串（BCrypt 约 60 字符）
     * @throws IllegalArgumentException 如果参数为 null 或空
     */
    String hash(String rawPassword);

    /**
     * 验证明文密码与散列值是否匹配。
     *
     * @param rawPassword 明文密码
     * @param hashedValue 散列值
     * @return true 匹配
     */
    boolean matches(String rawPassword, String hashedValue);
}
