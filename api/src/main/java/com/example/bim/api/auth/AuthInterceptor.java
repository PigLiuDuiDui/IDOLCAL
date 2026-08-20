package com.example.bim.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 管理权限拦截：保护管理接口（/api/admin/**）与数据写操作（POST/PUT/PATCH/DELETE）。
 * 要求 Authorization: Bearer <JWT> 且角色为 ADMIN；开关 idolcal.auth.enabled=false 可整体关闭。
 * /api/admin/login 放行（登录本身无需 token）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService auth;
    private final boolean enabled;

    public AuthInterceptor(AuthService auth, @Value("${idolcal.auth.enabled:true}") boolean enabled) {
        this.auth = auth;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) return true;
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean adminApi = path.startsWith("/api/admin/") && !path.equals("/api/admin/login");
        boolean dataWrite = isDataWrite(path, method);
        if (!adminApi && !dataWrite) return true;

        String token = bearerToken(request);
        if (!auth.isAdmin(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Admin authentication required\"}");
            return false;
        }
        return true;
    }

    /** 活动 / 艺人 / 回归 / 教程的写操作（管理端数据维护） */
    private boolean isDataWrite(String path, String method) {
        if (!method.equals("POST") && !method.equals("PUT") && !method.equals("PATCH") && !method.equals("DELETE")) {
            return false;
        }
        return path.startsWith("/api/events")
                || path.startsWith("/api/artists")
                || path.startsWith("/api/comebacks")
                || path.startsWith("/api/tutorials");
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7).trim();
    }
}
