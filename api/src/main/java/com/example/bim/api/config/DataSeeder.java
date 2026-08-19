package com.example.bim.api.config;

import com.example.bim.api.dto.ArtistDto;
import com.example.bim.api.dto.ComebackDto;
import com.example.bim.api.dto.EventDto;
import com.example.bim.api.dto.TutorialDto;
import com.example.bim.api.entity.Artist;
import com.example.bim.api.entity.Comeback;
import com.example.bim.api.entity.Event;
import com.example.bim.api.entity.Meta;
import com.example.bim.api.entity.Tutorial;
import com.example.bim.api.repository.ArtistRepository;
import com.example.bim.api.repository.ComebackRepository;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.repository.MetaRepository;
import com.example.bim.api.repository.TutorialRepository;
import com.example.bim.api.service.ArtistService;
import com.example.bim.api.service.ComebackService;
import com.example.bim.api.service.EventService;
import com.example.bim.api.service.TutorialService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 首次启动种子导入：表为空时读取 classpath:seed/*.json 写入数据库。
 * 数据由前端脚本生成：npm run export:seed（api/src/main/resources/seed/）。
 * 之后数据以数据库为准，重新导出需在管理端确认或清库。
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final EventService eventService;
    private final ArtistService artistService;
    private final ComebackService comebackService;
    private final TutorialService tutorialService;
    private final EventRepository eventRepo;
    private final ArtistRepository artistRepo;
    private final ComebackRepository comebackRepo;
    private final TutorialRepository tutorialRepo;
    private final MetaRepository metaRepo;
    private final ObjectMapper mapper;

    public DataSeeder(EventService eventService, ArtistService artistService,
                      ComebackService comebackService, TutorialService tutorialService,
                      EventRepository eventRepo, ArtistRepository artistRepo,
                      ComebackRepository comebackRepo, TutorialRepository tutorialRepo,
                      MetaRepository metaRepo, ObjectMapper mapper) {
        this.eventService = eventService;
        this.artistService = artistService;
        this.comebackService = comebackService;
        this.tutorialService = tutorialService;
        this.eventRepo = eventRepo;
        this.artistRepo = artistRepo;
        this.comebackRepo = comebackRepo;
        this.tutorialRepo = tutorialRepo;
        this.metaRepo = metaRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        boolean fresh = eventRepo.count() == 0 && artistRepo.count() == 0
                && comebackRepo.count() == 0 && tutorialRepo.count() == 0;
        if (!fresh) {
            log.info("[seed] 数据库已有数据，跳过种子导入");
            return;
        }
        log.info("[seed] 空库，开始导入种子数据…");

        seedEvents();
        seedArtists();
        seedComebacks();
        seedTutorials();
        seedMeta();

        log.info("[seed] 导入完成：events={} artists={} comebacks={} tutorials={}",
                eventRepo.count(), artistRepo.count(), comebackRepo.count(), tutorialRepo.count());
    }

    private void seedEvents() throws Exception {
        List<EventDto> dtos = read("seed/events.json", new TypeReference<>() {});
        for (EventDto dto : dtos) {
            eventService.create(dto);
        }
    }

    private void seedArtists() throws Exception {
        List<ArtistDto> dtos = read("seed/artists.json", new TypeReference<>() {});
        for (ArtistDto dto : dtos) {
            artistService.create(dto);
        }
    }

    private void seedComebacks() throws Exception {
        List<ComebackDto> dtos = read("seed/comebacks.json", new TypeReference<>() {});
        for (ComebackDto dto : dtos) {
            comebackService.create(dto);
        }
    }

    private void seedTutorials() throws Exception {
        List<TutorialDto> dtos = read("seed/tutorials.json", new TypeReference<>() {});
        for (TutorialDto dto : dtos) {
            tutorialService.create(dto);
        }
    }

    private void seedMeta() throws Exception {
        // Jackson 3 的 JsonNode 已移除 fields()，直接按 Map 读取遍历
        Map<String, JsonNode> meta = read("seed/meta.json", new TypeReference<>() {});
        for (Map.Entry<String, JsonNode> entry : meta.entrySet()) {
            Meta m = new Meta();
            m.setMetaKey(entry.getKey());
            try {
                m.setMetaValue(mapper.writeValueAsString(entry.getValue()));
            } catch (Exception e) {
                throw new IllegalStateException("meta seed corrupt: " + entry.getKey(), e);
            }
            metaRepo.save(m);
        }
    }

    private <T> T read(String path, TypeReference<T> type) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        }
    }
}
