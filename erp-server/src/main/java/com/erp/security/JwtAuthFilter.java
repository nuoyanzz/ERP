package com.erp.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(properties.getHeader());
        if (StringUtils.hasText(header) && header.startsWith(properties.getTokenPrefix())) {
            String token = header.substring(properties.getTokenPrefix().length());
            try {
                Claims claims = jwtUtil.parseToken(token);
                // 刷新令牌只能用于换取访问令牌，不能直接访问业务接口
                Object tokenType = claims.get(JwtUtil.CLAIM_TOKEN_TYPE);
                if (tokenType != null && !JwtUtil.TOKEN_TYPE_ACCESS.equals(tokenType)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                String username = claims.getSubject();
                Object userId = claims.get(JwtUtil.CLAIM_USER_ID);
                Object nickname = claims.get(JwtUtil.CLAIM_NICKNAME);
                List<String> perms = new ArrayList<>();
                Object permsClaim = claims.get(JwtUtil.CLAIM_PERMS);
                if (permsClaim instanceof List<?>) {
                    for (Object item : (List<?>) permsClaim) {
                        perms.add(String.valueOf(item));
                    }
                }
                if (StringUtils.hasText(username)) {
                    LoginUser loginUser = LoginUser.builder()
                            .userId(userId == null ? null : Long.valueOf(String.valueOf(userId)))
                            .username(username)
                            .nickname(nickname == null ? null : String.valueOf(nickname))
                            .perms(perms)
                            .build();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}








