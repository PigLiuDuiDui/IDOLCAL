package com.example.bim.api.service;

import com.example.bim.api.cache.CacheService;
import com.example.bim.api.entity.Meta;
import com.example.bim.api.repository.MetaRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 元数据服务：活动类型 / 状态 / 来源可信度 / 回归节点定义。
 * 前端 /api/meta 拉取后用于筛选器、类型标记、回归时间线节点。
 * 全量结果走 Redis 缓存（15s TTL），写操作立即失效。
 */
@Service
public class MetaService {

    private static final String CACHE_KEY = "cache:meta";
    private static final TypeReference<Map<String, JsonNode>> META_TYPE = new TypeReference<>() {};

    private final MetaRepository repo;
    private final ObjectMapper mapper;
    private final CacheService cache;
    private final WebPushService webPush;

    public MetaService(MetaRepository repo, ObjectMapper mapper, CacheService cache, WebPushService webPush) {
        this.repo = repo;
        this.mapper = mapper;
        this.cache = cache;
        this.webPush = webPush;
    }

    /** 合并所有 meta 为单一对象：{ eventTypes, statuses, sourceLevels, comebackStages } */
    @Transactional(readOnly = true)
    public Map<String, JsonNode> getAll() {
        Map<String, JsonNode> cached = cache.get(CACHE_KEY, META_TYPE);
        if (cached != null) return cached;
        Map<String, JsonNode> out = new LinkedHashMap<>();
        for (Meta m : repo.findAll()) {
            out.put(m.getMetaKey(), parse(m.getMetaValue()));
        }
        cache.put(CACHE_KEY, out, Duration.ofSeconds(15));
        return out;
    }

    /** 更新单个 key（如管理端调整活动类型） */
    @Transactional
    public JsonNode put(String key, JsonNode value) {
        Meta m = repo.findById(key).orElseGet(() -> {
            Meta n = new Meta();
            n.setMetaKey(key);
            return n;
        });
        try {
            m.setMetaValue(mapper.writeValueAsString(value));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("invalid JSON for meta key: " + key);
        }
        JsonNode saved = parse(repo.save(m).getMetaValue());
        cache.evict(CACHE_KEY);
        // eventTypes 变更后推送通知的类型标签同步重载，避免发送路径读到旧标签
        if ("eventTypes".equals(key)) webPush.invalidateTypeLabels();
        return saved;
    }

    private JsonNode parse(String s) {
        try {
            return mapper.readTree(s);
        } catch (JacksonException e) {
            throw new IllegalStateException("meta value corrupt: " + s);
        }
    }
}
