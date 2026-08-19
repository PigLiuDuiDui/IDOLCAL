package com.example.bim.api.ratelimit;

import java.util.Set;

/**
 * 限流规则：Ant 风格路径 + HTTP 方法 → 限额。
 * windowSeconds 窗口内同一 IP 最多 limit 次请求，超限返回 429。
 */
public record RateLimitRule(String pattern, Set<String> methods, int limit, int windowSeconds) {
}
