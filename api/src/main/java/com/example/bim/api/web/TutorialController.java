package com.example.bim.api.web;

import com.example.bim.api.dto.TutorialDto;
import com.example.bim.api.service.TutorialService;
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
 * 教程板块管理接口（打榜 / 数据教程）
 * GET /api/tutorials 列表（含 ready 与 coming 板块）｜其余标准 CRUD
 */
@RestController
@RequestMapping("/api/tutorials")
public class TutorialController {

    private final TutorialService service;

    public TutorialController(TutorialService service) {
        this.service = service;
    }

    @GetMapping
    public List<TutorialDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public TutorialDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TutorialDto create(@Valid @RequestBody TutorialDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public TutorialDto update(@PathVariable String id, @Valid @RequestBody TutorialDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
