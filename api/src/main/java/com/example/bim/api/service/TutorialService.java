package com.example.bim.api.service;

import com.example.bim.api.dto.TutorialDto;
import com.example.bim.api.entity.Tutorial;
import com.example.bim.api.repository.TutorialRepository;
import com.example.bim.api.Exception.BadRequestException;
import com.example.bim.api.Exception.NotFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 教程板块 CRUD。sections/notes/noCount 为 JSON 数组，实体层存字符串。 */
@Service
public class TutorialService {

    private static final JsonNode EMPTY_ARRAY = new ObjectMapper().createArrayNode();

    private final TutorialRepository repo;
    private final ObjectMapper mapper;

    public TutorialService(TutorialRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TutorialDto> list() {
        return repo.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TutorialDto get(String id) {
        return toDto(find(id));
    }

    @Transactional
    public TutorialDto create(TutorialDto dto) {
        validate(dto);
        if (dto.id() == null || dto.id().isBlank()) throw new BadRequestException("tutorial id is required");
        if (repo.existsById(dto.id())) throw new BadRequestException("Tutorial already exists: " + dto.id());

        Tutorial t = new Tutorial();
        t.setId(dto.id());
        apply(t, dto);
        return toDto(repo.save(t));
    }

    @Transactional
    public TutorialDto update(String id, TutorialDto dto) {
        validate(dto);
        Tutorial t = find(id);
        apply(t, dto);
        return toDto(repo.save(t));
    }

    @Transactional
    public void delete(String id) {
        Tutorial t = find(id);
        repo.delete(t);
    }

    // ---- 内部 ----

    private Tutorial find(String id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Tutorial not found: " + id));
    }

    private void validate(TutorialDto dto) {
        if (dto.title() == null || dto.title().isBlank()) throw new BadRequestException("title is required");
        if (dto.status() == null || dto.status().isBlank()) throw new BadRequestException("status is required");
    }

    private void apply(Tutorial t, TutorialDto dto) {
        t.setStatus(dto.status());
        t.setTitle(dto.title());
        t.setTagline(dto.tagline());
        t.setSectionsJson(json(dto.sections()));
        t.setNotesJson(json(dto.notes()));
        t.setNoCountJson(json(dto.noCount()));
    }

    /** JsonNode → 字符串；null 存 "[]" 保持结构一致 */
    private String json(JsonNode node) {
        if (node == null || node.isNull()) return "[]";
        try {
            return mapper.writeValueAsString(node);
        } catch (JacksonException e) {
            throw new BadRequestException("invalid JSON: " + e.getMessage());
        }
    }

    private TutorialDto toDto(Tutorial t) {
        return new TutorialDto(
                t.getId(),
                t.getStatus(),
                t.getTitle(),
                t.getTagline(),
                parse(t.getSectionsJson()),
                parse(t.getNotesJson()),
                parse(t.getNoCountJson())
        );
    }

    private JsonNode parse(String s) {
        if (s == null || s.isBlank()) return EMPTY_ARRAY;
        try {
            return mapper.readTree(s);
        } catch (JacksonException e) {
            return EMPTY_ARRAY;
        }
    }
}
