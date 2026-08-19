package com.example.bim.api.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 热门接口缓存（Redis String + JSON）。Redis 故障时自动跳过缓存（直查库），
 * 30 秒后自动重探；开关：idolcal.cache.enabled=false。
 * 管理端写操作调用 evict() 立即失效，TTL 仅为兜底。
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final long RETRY_AFTER_MS = 30_000;

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final AtomicBoolean available = new AtomicBoolean(true);
    private final AtomicLong lastFailure = new AtomicLong(0);

    public CacheService(StringRedisTemplate redis,
                        ObjectMapper mapper,
                        @Value("${idolcal.cache.enabled:true}") boolean enabled) {
        this.redis = redis;
        this.mapper = mapper;
        this.enabled = enabled;
    }

    /** 读取缓存 JSON → 对象；未命中或 Redis 故障返回 null */
    public <T> T get(String key, TypeReference<T> type) {
        if (!enabled || !canTry()) return null;
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : mapper.readValue(json, type);
        } catch (Exception e) {
            markDown(e);
            return null;
        }
    }

    /** 写入缓存（序列化失败或 Redis 故障时静默跳过，不影响主流程） */
    public void put(String key, Object value, Duration ttl) {
        if (!enabled || !canTry()) return;
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            markDown(e);
        }
    }

    /** 立即失效（管理端写操作调用） */
    public void evict(String key) {
        if (!enabled || !canTry()) return;
        try {
            redis.delete(key);
        } catch (Exception e) {
            markDown(e);
        }
    }

    /** Redis 故障后：30 秒内不再尝试，之后自动重探 */
    private boolean canTry() {
        if (available.get()) return true;
        return System.currentTimeMillis() - lastFailure.get() > RETRY_AFTER_MS;
    }

    private void markDown(Exception e) {
        if (available.compareAndSet(true, false)) {
            log.warn("[cache] Redis 操作失败，缓存降级为直查库（30 秒后重探）：{}", e.getMessage());
        }
        lastFailure.set(System.currentTimeMillis());
    }
}
