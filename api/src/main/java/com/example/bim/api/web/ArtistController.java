package com.example.bim.api.web;

import com.example.bim.api.auth.AdminOnly;
import com.example.bim.api.dto.ArtistDto;
import com.example.bim.api.service.ArtistService;
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
 * 艺人档案管理接口
 * GET /api/artists 列表（current=true 为当前展示艺人）｜其余标准 CRUD
 */
@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private final ArtistService service;

    public ArtistController(ArtistService service) {
        this.service = service;
    }

    @GetMapping
    public List<ArtistDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ArtistDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AdminOnly
    public ArtistDto create(@Valid @RequestBody ArtistDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @AdminOnly
    public ArtistDto update(@PathVariable String id, @Valid @RequestBody ArtistDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AdminOnly
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
