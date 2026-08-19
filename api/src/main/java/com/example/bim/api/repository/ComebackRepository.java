package com.example.bim.api.repository;

import com.example.bim.api.entity.Comeback;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComebackRepository extends JpaRepository<Comeback, String> {

    @EntityGraph(attributePaths = "stages")
    List<Comeback> findAllByOrderByReleaseDateAsc();

    @EntityGraph(attributePaths = "stages")
    Optional<Comeback> findWithStagesById(String id);
}
