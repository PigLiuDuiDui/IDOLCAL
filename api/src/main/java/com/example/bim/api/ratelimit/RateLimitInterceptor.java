package com.example.bim.api.ratelimit;

import com.example.bim.api.Exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 限流拦截器：按规则表（路径 + 方法）匹配，同一 IP 在同一窗口内共享额度。
 * bucketKey 按“窗口 + 方法 + 规则模式 + IP”归一化，避免遍历路径参数绕过限流。
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter limiter;
    private final List<RateLimitRule> rules;
    private final boolean enabled;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RateLimitInterceptor(RateLimiter limiter, List<RateLimitRule> rules, boolean enabled) {
        this.limiter = limiter;
        this.rules = rules;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || !request.getRequestURI().startsWith("/api/")) return true;
        for (RateLimitRule rule : rules) {
            if (!rule.methods().contains(request.getMethod())) continue;
            if (!matcher.match(rule.pattern(), request.getRequestURI())) continue;
            String key = bucketKey(request, rule);
            if (!limiter.tryAcquire(key, rule.limit(), rule.windowSeconds())) {
                throw new RateLimitException(limiter.retryAfterSeconds(key, rule.windowSeconds()));
            }
            return true;
        }
        return true;
    }

    /** 同一规则同一 IP 共享额度：路径参数（如 e001）不参与 key */
    private String bucketKey(HttpServletRequest request, RateLimitRule rule) {
        return "rl:" + rule.windowSeconds() + ":" + request.getMethod() + ":" + rule.pattern() + ":" + clientIp(request);
    }

    /**
     * 客户端 IP：优先 X-Forwarded-For 首个值（部署时由 CDN/WAF 设置并清理伪造头）；
     * 否则取远端地址（本地开发即 127.0.0.1）。
     */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
