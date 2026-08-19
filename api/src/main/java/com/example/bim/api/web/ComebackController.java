package com.example.bim.api.web;

import com.example.bim.api.dto.ComebackDto;
import com.example.bim.api.service.ComebackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 回归专题管理接口。stages 节点引用 events 表的活动 id（不重复创建活动数据）。
 * GET /api/comebacks 列表（按发行日升序）｜其余标准 CRUD
 */
@RestController
@RequestMapping("/api/comebacks")
public class ComebackController {

    private final ComebackService service;

    public ComebackController(ComebackService service) {
        this.service = service;
    }

    @GetMapping
    public List<ComebackDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ComebackDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComebackDto create(@Valid @RequestBody ComebackDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ComebackDto update(@PathVariable String id, @Valid @RequestBody ComebackDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
