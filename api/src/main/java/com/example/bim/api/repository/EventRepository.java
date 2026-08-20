package com.example.bim.api.repository;

import com.example.bim.api.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findAllByOrderByDateAscTimeAsc();

    /** 启动迁移用：补算升级前缺失的 start_at_utc */
    List<Event> findByStartAtUtcIsNull();

    /** 仅投影 id 列（nextId 计算用，避免全表加载实体） */
    @Query("select e.id from Event e")
    List<String> findAllIds();

    boolean existsByArtist(String artistId);
}
