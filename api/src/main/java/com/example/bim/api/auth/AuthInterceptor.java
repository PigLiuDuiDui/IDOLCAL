package com.example.bim.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 管理权限拦截：保护 @AdminOnly 标注的接口（方法或类级）与 /api/admin/** 命名空间。
 * 要求 Authorization: Bearer <JWT> 且角色为 ADMIN；开关 idolcal.auth.enabled=false 可整体关闭。
 * /api/admin/login 放行（登录本身无需 token）；数据写接口一律显式标注 @AdminOnly，
 * 不再按路径前缀猜测（避免新增相似前缀路由被误拦 / 漏拦）。
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

        boolean annotated = false;
        if (handler instanceof HandlerMethod hm) {
            // 方法级注解优先；类级注解作用于该类全部方法（如 AdminController 除 login 外逐方法标注）
            annotated = hm.hasMethodAnnotation(AdminOnly.class)
                    || hm.getBeanType().isAnnotationPresent(AdminOnly.class);
        }
        // /api/admin/** 命名空间兜底：未来新加管理接口忘标注解时不裸奔（login 除外）
        boolean adminApi = path.startsWith("/api/admin/") && !path.equals("/api/admin/login");
        if (!annotated && !adminApi) return true;

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

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        return header.substring(7).trim();
    }
}
