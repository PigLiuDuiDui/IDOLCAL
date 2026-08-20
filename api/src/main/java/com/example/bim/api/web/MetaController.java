package com.example.bim.api.web;

import com.example.bim.api.auth.AdminOnly;
import com.example.bim.api.service.MetaService;
import tools.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 元数据接口（活动类型 / 状态 / 来源可信度 / 回归节点定义）
 * GET /api/meta 返回 { eventTypes, statuses, sourceLevels, comebackStages }
 * PUT /api/meta/{key} 更新单个 key（管理端使用）
 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final MetaService service;

    public MetaController(MetaService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, JsonNode> getAll() {
        return service.getAll();
    }

    @PutMapping("/{key}")
    @AdminOnly
    public JsonNode put(@PathVariable String key, @RequestBody JsonNode value) {
        return service.put(key, value);
    }
}
