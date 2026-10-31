package com.erp.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil(new JwtProperties());

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
}








