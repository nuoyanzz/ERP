package com.erp.security.datascope;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 数据权限请求级过滤器。
 * <p>
 * 在请求开始时解析一次数据范围并放入上下文，请求结束时清理，
 * 避免同一次请求内多次查询重复解析权限。
 */
@Order(1)
@Component
@RequiredArgsConstructor
public class DataScopeRequestFilter extends OncePerRequestFilter {

    private final DataScopeResolver dataScopeResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            DataScopeContext.DataScopeInfo info = dataScopeResolver.resolveCurrent();
            if (info != null) {
                DataScopeContext.set(info);
            }
            filterChain.doFilter(request, response);
        } finally {
            DataScopeContext.clear();
        }
    }
}








