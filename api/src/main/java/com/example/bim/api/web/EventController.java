package com.example.bim.api.web;

import com.example.bim.api.auth.AdminOnly;
import com.example.bim.api.dto.EventDto;
import com.example.bim.api.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动管理接口（IdolCal 数据管理，配合前端日历 / 提醒 / 回归专题使用）
 * GET    /api/events            列表（可选 ?type=&status=&artist=&from=&to=&page=&size=；分页不传默认全量）
 * GET    /api/events/{id}       详情
 * POST   /api/events            新增（id 缺省时自动生成 e###）
 * PUT    /api/events/{id}       全量更新
 * DELETE /api/events/{id}       删除
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @GetMapping
    public List<EventDto> list(@RequestParam(required = false) String type,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String artist,
                               @RequestParam(required = false) String from,
                               @RequestParam(required = false) String to,
                               @RequestParam(required = false) Integer page,
                               @RequestParam(required = false) Integer size) {
        return service.list(type, status, artist, from, to, page, size);
    }

    @GetMapping("/{id}")
    public EventDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AdminOnly
    public EventDto create(@Valid @RequestBody EventDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @AdminOnly
    public EventDto update(@PathVariable String id, @Valid @RequestBody EventDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AdminOnly
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
