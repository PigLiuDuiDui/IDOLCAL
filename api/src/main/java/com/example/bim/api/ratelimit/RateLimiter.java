package com.example.bim.api.ratelimit;

/**
 * 限流器抽象：Redis 版（计数共享、多实例一致）与内存版（单实例降级）二选一，
 * 由 RateLimitConfig 启动时探测 Redis 决定。业务代码只依赖本接口。
 */
public interface RateLimiter {

    /** 尝试获取一个配额，true = 放行，false = 已超限 */
    boolean tryAcquire(String bucketKey, int limit, int windowSeconds);

    /** 当前桶剩余冷却秒数（超限响应 Retry-After 用） */
    long retryAfterSeconds(String bucketKey, int windowSeconds);

    /** 实现名（日志 / 排查用） */
    String name();
}
