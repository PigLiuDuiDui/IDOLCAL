package com.example.bim.api.repository;

import com.example.bim.api.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, String> {

    Optional<Artist> findFirstByCurrentTrue();
}
