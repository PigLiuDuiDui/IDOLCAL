package com.example.bim.api.auth;

import com.example.bim.api.entity.User;
import com.example.bim.api.repository.UserRepository;
import com.example.bim.api.web.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理后台认证：登录签发 JWT / token 校验。
 * 账号为 users 表 role=ADMIN 的用户（启动时由 AdminBootstrap 创建）。
 * 登录安全：按「用户名 + IP」维度连续失败锁定（默认 5 次 / 锁 15 分钟），
 * 配合 RateLimitConfig 的 /api/admin/login 专用限流（10 次/分钟/IP）双保险。
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final JwtUtil jwt;
    private final int maxFailures;
    private final long lockMillis;

    /** 登录失败守卫（内存实现；多实例部署时建议换成 Redis 计数） */
    private final Map<String, LoginGuard> guards = new ConcurrentHashMap<>();

    public AuthService(UserRepository users, JwtUtil jwt,
                       @Value("${idolcal.auth.login-max-failures:5}") int maxFailures,
                       @Value("${idolcal.auth.login-lock-minutes:15}") long lockMinutes) {
        this.users = users;
        this.jwt = jwt;
        this.maxFailures = Math.max(1, maxFailures);
        this.lockMillis = Math.max(1, lockMinutes) * 60_000L;
    }

    /** 登录校验，成功返回 JWT（连续失败锁定；ip 用于失败维度统计） */
    public String login(String username, String password, String ip) {
        LoginGuard guard = guards.computeIfAbsent(guardKey(username, ip), k -> new LoginGuard(maxFailures, lockMillis));
        if (guard.locked(System.currentTimeMillis())) {
            throw new UnauthorizedException("Too many failed attempts, account temporarily locked");
        }
        User user = users.findByUsername(username).orElse(null);
        if (user == null || user.getPasswordHash() == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            guard.fail(System.currentTimeMillis());
            throw new UnauthorizedException("Invalid username or password");
        }
        guard.reset();
        user.setLastLoginAt(System.currentTimeMillis());
        users.save(user);
        return jwt.createToken(user.getUsername(), user.getRole());
    }

    /** token 是否有效且为 ADMIN 角色 */
    public boolean isAdmin(String token) {
        if (token == null || token.isBlank()) return false;
        Map<String, String> claims = jwt.verify(token);
        return claims != null && "ADMIN".equals(claims.get("role"));
    }

    private String guardKey(String username, String ip) {
        return username + "|" + (ip == null ? "" : ip);
    }

    /** 内存登录守卫：连续失败达到阈值后锁定 lockMillis 毫秒 */
    private static final class LoginGuard {
        private final int maxFailures;
        private final long lockMillis;
        private int failures;
        private long lockUntil;

        LoginGuard(int maxFailures, long lockMillis) {
            this.maxFailures = maxFailures;
            this.lockMillis = lockMillis;
        }

        synchronized boolean locked(long now) {
            return lockUntil > now;
        }

        synchronized void fail(long now) {
            failures++;
            if (failures >= maxFailures) {
                lockUntil = now + lockMillis;
                failures = 0;
            }
        }

        synchronized void reset() {
            failures = 0;
            lockUntil = 0;
        }
    }
}
