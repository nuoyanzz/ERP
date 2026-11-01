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
        String secret = properties.getSecret();
        // 空密钥或过短密钥会让令牌可被伪造，必须在签发前拦截
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT 密钥未配置，请通过 JWT_SECRET 环境变量设置");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥长度不足 32 字节，请通过 JWT_SECRET 环境变量设置足够长的密钥");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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








