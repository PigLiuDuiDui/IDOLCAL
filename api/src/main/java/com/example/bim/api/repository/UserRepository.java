package com.example.bim.api.repository;

import com.example.bim.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /** 后台用户搜索（username 模糊，不区分大小写） */
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
}
