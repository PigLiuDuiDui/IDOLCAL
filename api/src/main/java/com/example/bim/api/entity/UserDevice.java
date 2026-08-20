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
 * 用户设备（浏览器 / PWA 安装实例的注册记录）
 * 订阅或同步提醒时自动 upsert；user_id 为空 = 匿名模式，
 * 未来登录后绑定到 User 即可实现多设备同步。
 */
@Entity
@Table(name = "user_devices")
@Getter
@Setter
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设备标识（前端 localStorage 生成，唯一） */
    @Column(nullable = false, length = 64, unique = true)
    private String deviceId;

    /** 绑定的用户（可空 = 匿名） */
    @Column(name = "user_id")
    private Long userId;

    /** 设备平台：PC / Android / iOS PWA 等（由订阅请求 UA 推断） */
    @Column(length = 16)
    private String platform;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "last_active_at", nullable = false)
    private long lastActiveAt;
}
