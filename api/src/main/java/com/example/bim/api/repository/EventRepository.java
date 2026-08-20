package com.example.bim.api.repository;

import com.example.bim.api.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findAllByOrderByDateAscTimeAsc();

    /** 启动迁移用：补算升级前缺失的 start_at_utc */
    List<Event> findByStartAtUtcIsNull();

    boolean existsByArtist(String artistId);
}
