package com.example.bim.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 安全响应头（轻量替代 Spring Security 的默认头）：
 * - nosniff：禁止 MIME 类型嗅探
 * - DENY：禁止被 iframe 嵌套（防点击劫持）
 * - Referrer-Policy：外链不携带完整地址
 * - Permissions-Policy：关闭非必要浏览器能力
 * - API 响应 no-store：敏感数据不进浏览器缓存
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            response.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }
}
