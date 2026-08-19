package com.example.bim.api.service;

import com.example.bim.api.dto.ArtistDto;
import com.example.bim.api.entity.Artist;
import com.example.bim.api.repository.ArtistRepository;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.web.BadRequestException;
import com.example.bim.api.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 艺人档案 CRUD。current 艺人切换时互斥；删除当前艺人需先切换。 */
@Service
public class ArtistService {

    private final ArtistRepository repo;
    private final EventRepository eventRepo;

    public ArtistService(ArtistRepository repo, EventRepository eventRepo) {
        this.repo = repo;
        this.eventRepo = eventRepo;
    }

    @Transactional(readOnly = true)
    public List<ArtistDto> list() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ArtistDto get(String id) {
        return toDto(find(id));
    }

    @Transactional
    public ArtistDto create(ArtistDto dto) {
        if (dto.id() == null || dto.id().isBlank()) throw new BadRequestException("artist id is required");
        if (dto.name() == null || dto.name().isBlank()) throw new BadRequestException("name is required");
        if (repo.existsById(dto.id())) throw new BadRequestException("Artist already exists: " + dto.id());

        Artist a = new Artist();
        a.setId(dto.id());
        apply(a, dto);
        if (a.isCurrent()) clearCurrentExcept(a.getId());
        return toDto(repo.save(a));
    }

    @Transactional
    public ArtistDto update(String id, ArtistDto dto) {
        Artist a = find(id);
        apply(a, dto);
        if (a.isCurrent()) clearCurrentExcept(a.getId());
        return toDto(repo.save(a));
    }

    @Transactional
    public void delete(String id) {
        Artist a = find(id);
        if (a.isCurrent()) throw new BadRequestException("Cannot delete the current artist; switch to another artist first");
        if (eventRepo.existsByArtist(id)) throw new BadRequestException("Artist still has events; delete or reassign them first");
        repo.delete(a);
    }

    // ---- 内部 ----

    private Artist find(String id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Artist not found: " + id));
    }

    /** 同一时刻只有一个 current 艺人 */
    private void clearCurrentExcept(String keepId) {
        repo.findAll().stream()
                .filter(a -> a.isCurrent() && !a.getId().equals(keepId))
                .forEach(a -> {
                    a.setCurrent(false);
                    repo.save(a);
                });
    }

    private void apply(Artist a, ArtistDto dto) {
        a.setName(dto.name());
        a.setSubName(dto.subName());
        a.setYear(dto.year());
        a.setEra(dto.era());
        a.setEraPeriod(dto.eraPeriod());
        a.setAccent(dto.accent());
        a.setAccentSoft(dto.accentSoft());
        a.setHeroImage(blankToNull(dto.heroImage()));
        a.setSourceTag(dto.sourceTag());
        a.setIntroEn(i18n(dto.intro(), "en"));
        a.setIntroZh(i18n(dto.intro(), "zh-CN"));
        a.setIntroKo(i18n(dto.intro(), "ko"));
        a.setCurrent(dto.current());
    }

    private ArtistDto toDto(Artist a) {
        return new ArtistDto(
                a.getId(),
                a.getName(),
                a.getSubName(),
                a.getYear(),
                a.getEra(),
                a.getEraPeriod(),
                a.getAccent(),
                a.getAccentSoft(),
                a.getHeroImage(),
                a.getSourceTag(),
                i18n(a.getIntroEn(), a.getIntroZh(), a.getIntroKo()),
                a.isCurrent()
        );
    }

    private Map<String, String> i18n(String en, String zh, String ko) {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        if (en != null) m.put("en", en);
        if (zh != null) m.put("zh-CN", zh);
        if (ko != null) m.put("ko", ko);
        return m.isEmpty() ? null : m;
    }

    private String i18n(Map<String, String> m, String key) {
        return m == null ? null : m.get(key);
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
