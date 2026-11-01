package com.erp.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    /** 测试专用密钥，长度满足 HS256 的下限要求 */
    private static final String TEST_SECRET = "unit-test-jwt-secret-key-at-least-32-bytes-long";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(TEST_SECRET);
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void generateAndParseToken() {
        List<String> perms = Arrays.asList("sys:user:query", "md:material:add");
        String token = jwtUtil.generateToken(1L, "admin", "超级管理员", perms);

        Claims claims = jwtUtil.parseToken(token);
        assertNotNull(claims);
        assertEquals("admin", claims.getSubject());
        assertEquals(1L, ((Number) claims.get(JwtUtil.CLAIM_USER_ID)).longValue());
        assertEquals("超级管理员", claims.get(JwtUtil.CLAIM_NICKNAME));
        assertEquals(perms, claims.get(JwtUtil.CLAIM_PERMS));
    }

    @Test
    void parseInvalidTokenThrows() {
        assertThrows(Exception.class, () -> jwtUtil.parseToken("invalid.token.value"));
    }

    @Test
    @DisplayName("未配置密钥时拒绝签发令牌")
    void missingSecretRejected() {
        JwtUtil noSecret = new JwtUtil(new JwtProperties());
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> noSecret.generateToken(1L, "admin", "管理员", List.of()));
        assertTrue(error.getMessage().contains("JWT 密钥未配置"));
    }

    @Test
    @DisplayName("密钥长度不足时拒绝签发令牌")
    void shortSecretRejected() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("too-short");
        JwtUtil shortSecret = new JwtUtil(properties);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> shortSecret.generateToken(1L, "admin", "管理员", List.of()));
        assertTrue(error.getMessage().contains("长度不足"));
    }
}
