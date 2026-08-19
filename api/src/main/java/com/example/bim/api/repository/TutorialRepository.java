package com.example.bim.api.repository;

import com.example.bim.api.entity.Tutorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TutorialRepository extends JpaRepository<Tutorial, String> {

    List<Tutorial> findAllByOrderByIdAsc();
}
