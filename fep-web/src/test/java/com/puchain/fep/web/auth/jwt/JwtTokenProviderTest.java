package com.puchain.fep.web.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 单元测试。
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        // 32 字节密钥 base64 编码
        props.setSecret("ZGV2LWRldi1kZXYtZGV2LWRldi1kZXYtZGV2LWRldi1kZXYtZGV2LWRldi0xMg==");
        props.setAccessTokenTtlSeconds(60L);
        props.setRefreshTokenTtlSeconds(300L);
        props.setIssuer("fep-test");
        provider = new JwtTokenProvider(props);
    }

    @Test
    void createAccessTokenShouldCarryRolesAndAccount() {
        String token = provider.createAccessToken("user-001", "admin", List.of("SYSTEM_ADMIN"));
        Claims claims = provider.parse(token);
        assertEquals("user-001", claims.getSubject());
        assertEquals("admin", claims.get("account"));
        assertEquals("ACCESS", claims.get("type"));
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertEquals(1, roles.size());
        assertEquals("SYSTEM_ADMIN", roles.get(0));
    }

    @Test
    void createRefreshTokenShouldHaveEmptyRolesAndRefreshType() {
        String token = provider.createRefreshToken("user-001", "admin");
        Claims claims = provider.parse(token);
        assertEquals("REFRESH", claims.get("type"));
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertTrue(roles.isEmpty());
    }

    @Test
    void expiredTokenShouldThrow() throws Exception {
        JwtProperties shortLived = new JwtProperties();
        shortLived.setSecret("ZGV2LWRldi1kZXYtZGV2LWRldi1kZXYtZGV2LWRldi1kZXYtZGV2LWRldi0xMg==");
        shortLived.setAccessTokenTtlSeconds(1L);
        shortLived.setIssuer("fep-test");
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortLived);

        String token = shortProvider.createAccessToken("user-001", "admin", List.of());
        Thread.sleep(1500);

        assertThrows(ExpiredJwtException.class, () -> shortProvider.parse(token));
    }

    @Test
    void tokenFromDifferentIssuerShouldBeRejected() {
        JwtProperties otherIssuer = new JwtProperties();
        otherIssuer.setSecret("ZGV2LWRldi1kZXYtZGV2LWRldi1kZXYtZGV2LWRldi1kZXYtZGV2LWRldi0xMg==");
        otherIssuer.setIssuer("other");
        JwtTokenProvider other = new JwtTokenProvider(otherIssuer);
        String token = other.createAccessToken("user-001", "admin", List.of());

        assertThrows(io.jsonwebtoken.IncorrectClaimException.class, () -> provider.parse(token));
    }

    @Test
    void extractJtiShouldReturnNonEmpty() {
        String token = provider.createAccessToken("user-001", "admin", List.of());
        String jti = provider.extractJti(token);
        assertNotNull(jti);
        assertFalse(jti.isBlank());
    }
}
