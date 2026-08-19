package com.example.bim.api.config;

import com.example.bim.api.ratelimit.InMemoryRateLimiter;
import com.example.bim.api.ratelimit.RateLimitInterceptor;
import com.example.bim.api.ratelimit.RateLimitRule;
import com.example.bim.api.ratelimit.RateLimiter;
import com.example.bim.api.ratelimit.RedisRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Set;

/**
 * 限流装配：启动时探测 Redis，可用则用 Redis 计数（多实例一致），
 * 不可用降级为内存实现并告警；规则表可按接口粒度调整限额。
 * 关闭开关：IDOLCAL_RATE_LIMIT_ENABLED=false
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    private final StringRedisTemplate redis;
    private final boolean enabled;

    public RateLimitConfig(StringRedisTemplate redis,
                           @Value("${idolcal.rate-limit.enabled:true}") boolean enabled) {
        this.redis = redis;
        this.enabled = enabled;
    }

    @Bean
    public RateLimiter rateLimiter() {
        try {
            redis.getConnectionFactory().getConnection().ping();
            log.info("[ratelimit] Redis 连接正常，使用 Redis 限流");
            return new RedisRateLimiter(redis);
        } catch (Exception e) {
            log.warn("[ratelimit] Redis 不可用（{}），限流降级为内存实现（仅单实例有效）", e.getMessage());
            return new InMemoryRateLimiter();
        }
    }

    @Bean
    public List<RateLimitRule> rateLimitRules() {
        return List.of(
                // 列表 / 元数据：60 次/分钟/IP
                new RateLimitRule("/api/events", Set.of("GET"), 60, 60),
                new RateLimitRule("/api/meta", Set.of("GET"), 60, 60),
                new RateLimitRule("/api/artists", Set.of("GET"), 60, 60),
                new RateLimitRule("/api/comebacks", Set.of("GET"), 60, 60),
                new RateLimitRule("/api/tutorials", Set.of("GET"), 60, 60),
                // 详情：120 次/分钟/IP
                new RateLimitRule("/api/events/*", Set.of("GET"), 120, 60),
                new RateLimitRule("/api/artists/*", Set.of("GET"), 120, 60),
                new RateLimitRule("/api/comebacks/*", Set.of("GET"), 120, 60),
                new RateLimitRule("/api/tutorials/*", Set.of("GET"), 120, 60),
                // 管理写操作：30 次/分钟/IP（未来接认证后改为按用户）
                new RateLimitRule("/api/**", Set.of("POST", "PUT", "PATCH", "DELETE"), 30, 60),
                // 兜底：其余 GET 60 次/分钟/IP
                new RateLimitRule("/api/**", Set.of("GET"), 60, 60)
        );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(rateLimiter(), rateLimitRules(), enabled))
                .addPathPatterns("/api/**");
    }
}
