package com.example.bim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户（当前为管理后台账号；未来接入登录后承载普通用户）
 * - 匿名模式：无账号用户不建行，仅通过 user_devices.device_id 锚定
 * - 管理后台：role=ADMIN 的账号，密码为 PBKDF2 哈希
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录名（唯一；匿名用户为 null） */
    @Column(length = 64, unique = true)
    private String username;

    /** 密码哈希（PBKDF2WithHmacSHA256，格式 salt:iterations:hash） */
    @Column(length = 300)
    private String passwordHash;

    /** 角色：USER / ADMIN */
    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, name = "created_at")
    private long createdAt;

    @Column(name = "last_login_at")
    private Long lastLoginAt;
}
