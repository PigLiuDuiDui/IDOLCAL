package com.example.bim.api.service;

import com.example.bim.api.entity.Comeback;
import com.example.bim.api.entity.ComebackStage;
import com.example.bim.api.repository.ComebackRepository;
import com.example.bim.api.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.bim.api.dto.ComebackDto;

/** 回归专题 CRUD。stages 全量替换（PUT 语义）。 */
@Service
public class ComebackService {

    private final ComebackRepository repo;

    public ComebackService(ComebackRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ComebackDto> list() {
        return repo.findAllByOrderByReleaseDateAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ComebackDto get(String id) {
        Comeback c = repo.findWithStagesById(id).orElseThrow(() -> new NotFoundException("Comeback not found: " + id));
        return toDto(c);
    }

    @Transactional
    public ComebackDto create(ComebackDto dto) {
        if (dto.id() == null || dto.id().isBlank()) {
            throw new IllegalArgumentException("comeback id is required");
        }
        if (repo.existsById(dto.id())) {
            throw new IllegalArgumentException("Comeback already exists: " + dto.id());
        }
        Comeback c = fromDto(dto);
        return toDto(repo.save(c));
    }

    @Transactional
    public ComebackDto update(String id, ComebackDto dto) {
        Comeback c = repo.findWithStagesById(id).orElseThrow(() -> new NotFoundException("Comeback not found: " + id));
        apply(c, dto);
        return toDto(repo.save(c));
    }

    @Transactional
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Comeback not found: " + id);
        }
        repo.deleteById(id);
    }

    // ---- 转换 ----

    private Comeback fromDto(ComebackDto dto) {
        Comeback c = new Comeback();
        c.setId(dto.id());
        apply(c, dto);
        return c;
    }

    private void apply(Comeback c, ComebackDto dto) {
        c.setArtistId(dto.artistId());
        c.setTitle(dto.title());
        c.setTaglineEn(i18n(dto.tagline(), "en"));
        c.setTaglineZh(i18n(dto.tagline(), "zh-CN"));
        c.setTaglineKo(i18n(dto.tagline(), "ko"));
        c.setReleaseDate(dto.releaseDate());
        c.setReleaseTime(dto.releaseTime());
        c.setReleaseTimezone(dto.releaseTimezone());

        c.getStages().clear();
        List<ComebackDto.Stage> stageDtos = dto.stages() == null ? List.of() : dto.stages();
        for (int i = 0; i < stageDtos.size(); i++) {
            ComebackDto.Stage s = stageDtos.get(i);
            ComebackStage stage = new ComebackStage();
            stage.setComeback(c);
            stage.setStageId(s.stage());
            stage.setSortOrder(i);
            stage.setEventIds(s.eventIds() == null ? "" : String.join(",", s.eventIds()));
            c.getStages().add(stage);
        }
    }

    private ComebackDto toDto(Comeback c) {
        return new ComebackDto(
                c.getId(),
                c.getArtistId(),
                c.getTitle(),
                i18n(c.getTaglineEn(), c.getTaglineZh(), c.getTaglineKo()),
                c.getReleaseDate(),
                c.getReleaseTime(),
                c.getReleaseTimezone(),
                c.getStages().stream()
                        .sorted(java.util.Comparator.comparingInt(ComebackStage::getSortOrder))
                        .map(s -> new ComebackDto.Stage(s.getStageId(), toList(s.getEventIds())))
                        .collect(Collectors.toList())
        );
    }

    /** i18n 三列 → Map（接口格式）；全空返回 null */
    private java.util.Map<String, String> i18n(String en, String zh, String ko) {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        if (en != null) m.put("en", en);
        if (zh != null) m.put("zh-CN", zh);
        if (ko != null) m.put("ko", ko);
        return m.isEmpty() ? null : m;
    }

    /** Map → 单列 */
    private String i18n(java.util.Map<String, String> m, String key) {
        return m == null ? null : m.get(key);
    }

    /** 逗号分隔 → List */
    private List<String> toList(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
