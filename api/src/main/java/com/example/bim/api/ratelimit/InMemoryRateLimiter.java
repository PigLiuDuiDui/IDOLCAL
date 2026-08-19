package com.example.bim.api.ratelimit;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存固定窗口限流：Redis 不可用时的降级实现（单实例有效）。
 * 窗口切换时旧 key 自然被覆盖；bucket 总量设上限，超出时清空防止长期运行内存膨胀。
 */
public class InMemoryRateLimiter implements RateLimiter {

    private static final int MAX_BUCKETS = 10_000;

    private record Window(long startEpochSecond, int count) {}

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String bucketKey, int limit, int windowSeconds) {
        long now = System.currentTimeMillis() / 1000;
        long windowStart = now - (now % windowSeconds);
        if (buckets.size() > MAX_BUCKETS) buckets.clear();
        Window w = buckets.compute(bucketKey, (k, cur) -> {
            if (cur == null || cur.startEpochSecond() != windowStart) return new Window(windowStart, 1);
            return new Window(windowStart, cur.count() + 1);
        });
        return w.count() <= limit;
    }

    @Override
    public long retryAfterSeconds(String bucketKey, int windowSeconds) {
        long now = System.currentTimeMillis() / 1000;
        long windowEnd = (now / windowSeconds + 1) * windowSeconds;
        return Math.max(1, windowEnd - now);
    }

    @Override
    public String name() {
        return "in-memory";
    }
}
