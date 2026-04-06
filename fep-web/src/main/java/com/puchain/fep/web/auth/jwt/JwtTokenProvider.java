package com.puchain.fep.web.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 签发和解析。
 *
 * <p>Token Claims:</p>
 * <ul>
 *   <li>{@code sub}: 用户 ID</li>
 *   <li>{@code iss}: 签发者（fep）</li>
 *   <li>{@code iat}: 签发时间</li>
 *   <li>{@code exp}: 过期时间</li>
 *   <li>{@code jti}: JWT ID（用于黑名单）</li>
 *   <li>{@code account}: 用户账号</li>
 *   <li>{@code roles}: 角色编码列表</li>
 *   <li>{@code type}: ACCESS / REFRESH</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    /**
     * 构造 JwtTokenProvider。
     *
     * @param properties JWT 配置属性
     */
    public JwtTokenProvider(final JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = Decoders.BASE64.decode(properties.getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 创建 Access Token。
     *
     * @param userId    用户 ID
     * @param account   用户账号
     * @param roleCodes 角色编码列表
     * @return JWT 字符串
     */
    public String createAccessToken(final String userId, final String account, final List<String> roleCodes) {
        return createToken(userId, account, roleCodes, "ACCESS", properties.getAccessTokenTtlSeconds());
    }

    /**
     * 创建 Refresh Token。
     *
     * @param userId  用户 ID
     * @param account 用户账号
     * @return JWT 字符串
     */
    public String createRefreshToken(final String userId, final String account) {
        return createToken(userId, account, List.of(), "REFRESH", properties.getRefreshTokenTtlSeconds());
    }

    private String createToken(final String userId, final String account,
                               final List<String> roleCodes, final String type, final long ttl) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("account", account);
        claims.put("roles", roleCodes);
        claims.put("type", type);
        return Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .claims(claims)
                .signWith(signingKey)
                .compact();
    }

    /**
     * 解析并验证 token，返回 Claims。
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims
     * @throws JwtException 验证失败（过期/签名错误/格式错误）
     */
    public Claims parse(final String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 token 提取 jti 用于黑名单。
     *
     * @param token JWT 字符串
     * @return JWT ID
     */
    public String extractJti(final String token) {
        return parse(token).getId();
    }
}
