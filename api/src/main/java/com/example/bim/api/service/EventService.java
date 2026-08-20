package com.example.bim.api.service;

import com.example.bim.api.cache.CacheService;
import com.example.bim.api.dto.EventDto;
import com.example.bim.api.entity.Event;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.util.EventTimes;
import com.example.bim.api.Exception.BadRequestException;
import com.example.bim.api.Exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 活动 CRUD。数据契约与前端 utils/time.js 的 getEventDateTime 一致：
 * date + time + timezone 为官方原始时区数据，不做任何转换；转换由前端完成。
 */
@Service
public class EventService {

    private static final Pattern NUMERIC_ID = Pattern.compile("^e(\\d+)$");
    /** 全量列表缓存（15s TTL；写操作立即失效） */
    private static final String CACHE_KEY = "cache:events";
    private static final TypeReference<List<EventDto>> EVENT_LIST_TYPE = new TypeReference<>() {};

    private final EventRepository repo;
    private final CacheService cache;

    public EventService(EventRepository repo, CacheService cache) {
        this.repo = repo;
        this.cache = cache;
    }

    /**
     * 列表（按日期时间升序），支持可选过滤；全量部分走 Redis 缓存。
     * page / size 同时提供时在过滤后内存分页（默认全量，兼容旧客户端）。
     */
    @Transactional(readOnly = true)
    public List<EventDto> list(String type, String status, String artist, String from, String to,
                               Integer page, Integer size) {
        List<EventDto> all = cache.get(CACHE_KEY, EVENT_LIST_TYPE);
        if (all == null) {
            all = repo.findAllByOrderByDateAscTimeAsc().stream().map(this::toDto).toList();
            cache.put(CACHE_KEY, all, Duration.ofSeconds(15));
        }
        List<EventDto> filtered = all.stream()
                .filter(e -> type == null || type.isBlank() || type.equals(e.type()))
                .filter(e -> status == null || status.isBlank() || status.equals(e.status()))
                .filter(e -> artist == null || artist.isBlank() || artist.equals(e.artist()))
                .filter(e -> from == null || from.isBlank() || e.date() == null || e.date().compareTo(from) >= 0)
                .filter(e -> to == null || to.isBlank() || e.date() == null || e.date().compareTo(to) <= 0)
                .toList();
        if (page == null || size == null || size <= 0) return filtered;
        int fromIdx = Math.max(0, page) * size;
        if (fromIdx >= filtered.size()) return List.of();
        return filtered.subList(fromIdx, Math.min(fromIdx + size, filtered.size()));
    }

    @Transactional(readOnly = true)
    public EventDto get(String id) {
        return toDto(find(id));
    }

    @Transactional
    public EventDto create(EventDto dto) {
        validate(dto, true);
        Event e = new Event();
        e.setId(dto.id() != null && !dto.id().isBlank() ? dto.id() : nextId());
        if (repo.existsById(e.getId())) {
            throw new BadRequestException("Event already exists: " + e.getId());
        }
        apply(e, dto);
        EventDto saved = toDto(repo.save(e));
        cache.evict(CACHE_KEY);
        return saved;
    }

    @Transactional
    public EventDto update(String id, EventDto dto) {
        validate(dto, false);
        Event e = find(id);
        apply(e, dto);
        EventDto saved = toDto(repo.save(e));
        cache.evict(CACHE_KEY);
        return saved;
    }

    @Transactional
    public void delete(String id) {
        Event e = find(id);
        repo.delete(e);
        cache.evict(CACHE_KEY);
    }

    // ---- 内部 ----

    private Event find(String id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Event not found: " + id));
    }

    private void validate(EventDto dto, boolean creating) {
        if (dto.artist() == null || dto.artist().isBlank()) throw new BadRequestException("artist is required");
        if (!isDate(dto.date())) throw new BadRequestException("date must be YYYY-MM-DD");
        if (dto.endDate() != null && !isDate(dto.endDate())) throw new BadRequestException("endDate must be YYYY-MM-DD");
        if (dto.time() != null && !dto.time().matches("^\\d{2}:\\d{2}$")) throw new BadRequestException("time must be HH:MM");
        if (dto.title() == null || dto.title().isEmpty()) throw new BadRequestException("title is required");
        if (dto.type() == null || dto.type().isBlank()) throw new BadRequestException("type is required");
        if (dto.status() == null || dto.status().isBlank()) throw new BadRequestException("status is required");
    }

    private boolean isDate(String s) {
        return s != null && s.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    /** 自动生成 id：现有 e### 最大序号 + 1（e025 …）；只投影 id 列，避免全表加载实体 */
    private String nextId() {
        int max = 0;
        for (String id : repo.findAllIds()) {
            Matcher m = NUMERIC_ID.matcher(id);
            if (m.matches()) max = Math.max(max, Integer.parseInt(m.group(1)));
        }
        return String.format("e%03d", max + 1);
    }

    private void apply(Event e, EventDto dto) {
        e.setArtist(dto.artist());
        e.setDate(dto.date());
        e.setEndDate(blankToNull(dto.endDate()));
        e.setTime(dto.time());
        e.setTimezone(dto.timezone());
        // 开始时刻由后端按官方时区计算（提醒调度 / 时区换算统一入口，前端不再各自计算）
        e.setStartAtUtc(EventTimes.startAtUtc(dto.date(), dto.time(), dto.timezone()));
        e.setTitleEn(i18n(dto.title(), "en"));
        e.setTitleZh(i18n(dto.title(), "zh-CN"));
        e.setTitleKo(i18n(dto.title(), "ko"));
        e.setType(dto.type());
        e.setStatus(dto.status());
        e.setLocationEn(i18n(dto.location(), "en"));
        e.setLocationZh(i18n(dto.location(), "zh-CN"));
        e.setLocationKo(i18n(dto.location(), "ko"));
        e.setDescriptionEn(i18n(dto.description(), "en"));
        e.setDescriptionZh(i18n(dto.description(), "zh-CN"));
        e.setDescriptionKo(i18n(dto.description(), "ko"));
        e.setImage(blankToNull(dto.image()));
        e.setSourceName(dto.sourceName());
        e.setSourceUrl(blankToNull(dto.sourceUrl()));
        e.setOfficial(dto.isOfficial());
        e.setOnlineUrl(blankToNull(dto.onlineUrl()));
        e.setMapUrl(blankToNull(dto.mapUrl()));
    }

    private EventDto toDto(Event e) {
        return new EventDto(
                e.getId(),
                e.getArtist(),
                e.getDate(),
                e.getEndDate(),
                e.getTime(),
                e.getTimezone(),
                i18n(e.getTitleEn(), e.getTitleZh(), e.getTitleKo()),
                e.getType(),
                e.getStatus(),
                i18n(e.getLocationEn(), e.getLocationZh(), e.getLocationKo()),
                i18n(e.getDescriptionEn(), e.getDescriptionZh(), e.getDescriptionKo()),
                e.getImage(),
                e.getSourceName(),
                e.getSourceUrl(),
                e.isOfficial(),
                e.getOnlineUrl(),
                e.getMapUrl()
        );
    }

    /** i18n 三列 → Map（接口格式）；全空返回 null */
    private Map<String, String> i18n(String en, String zh, String ko) {
        Map<String, String> m = new java.util.LinkedHashMap<>();
        if (en != null) m.put("en", en);
        if (zh != null) m.put("zh-CN", zh);
        if (ko != null) m.put("ko", ko);
        return m.isEmpty() ? null : m;
    }

    /** Map → 单列 */
    private String i18n(Map<String, String> m, String key) {
        return m == null ? null : m.get(key);
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
