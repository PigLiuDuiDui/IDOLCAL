package com.example.bim.api.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 固定窗口限流：INCR + EXPIRE 原子完成（Lua 脚本），
 * 计数在 Redis 内共享，多实例部署行为一致。
 * Redis 运行中故障时降级为“放行”并记录日志（限流是保护而非业务，不让 API 因限流器挂掉）。
 */
public class RedisRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    /** INCR 后首次置过期时间；返回当前计数 */
    private static final String ACQUIRE_LUA = """
            local c = redis.call('INCR', KEYS[1])
            if c == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return c
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script;
    private final AtomicBoolean degraded = new AtomicBoolean(false);

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        DefaultRedisScript<Long> s = new DefaultRedisScript<>();
        s.setScriptText(ACQUIRE_LUA);
        s.setResultType(Long.class);
        this.script = s;
    }

    @Override
    public boolean tryAcquire(String bucketKey, int limit, int windowSeconds) {
        if (degraded.get()) return true;
        try {
            Long count = redis.execute(script, List.of(bucketKey), String.valueOf(windowSeconds));
            return count == null || count <= limit;
        } catch (Exception e) {
            degraded.compareAndSet(false, true);
            log.warn("[ratelimit] Redis 操作失败，限流降级放行：{}", e.getMessage());
            return true;
        }
    }

    @Override
    public long retryAfterSeconds(String bucketKey, int windowSeconds) {
        try {
            Long ttl = redis.getExpire(bucketKey);
            if (ttl == null || ttl < 0) return windowSeconds;
            return Math.max(1, ttl);
        } catch (Exception e) {
            return windowSeconds;
        }
    }

    @Override
    public String name() {
        return "redis";
    }
}
