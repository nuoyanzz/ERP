package com.erp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_NICKNAME = "nickname";
    public static final String CLAIM_PERMS = "perms";
    /** 令牌类型：access 访问令牌 / refresh 刷新令牌 */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties properties;

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String nickname, List<String> perms) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpireHours() * 3600 * 1000);
        return Jwts.builder()
                .setSubject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_NICKNAME, nickname)
                .claim(CLAIM_PERMS, perms)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成刷新令牌。刷新令牌有效期长于访问令牌，用于换取新的访问令牌。
     */
    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getRefreshExpireDays() * 24 * 3600 * 1000);
        return Jwts.builder()
                .setSubject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 校验令牌类型，防止用刷新令牌直接访问业务接口。
     */
    public boolean isAccessToken(String token) {
        try {
            Object type = parseToken(token).get(CLAIM_TOKEN_TYPE);
            return TOKEN_TYPE_ACCESS.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Object type = parseToken(token).get(CLAIM_TOKEN_TYPE);
            return TOKEN_TYPE_REFRESH.equals(type);
        } catch (Exception e) {
            return false;
        }
    }
}








