package com.example.bim.api.repository;

import com.example.bim.api.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {

    List<Event> findAllByOrderByDateAscTimeAsc();

    boolean existsByArtist(String artistId);
}
