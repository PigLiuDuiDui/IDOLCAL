package com.example.bim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 浏览器 Push Subscription（Web Push 投递地址）
 * endpoint 为 Push Service 的投递 URL（唯一键，重复订阅时更新密钥）
 */
@Entity
@Table(name = "push_subscriptions", indexes = {
        // 按设备查订阅是发送热路径（sendReminder / sendTest），必须走索引
        @Index(name = "idx_push_subs_device", columnList = "device_id")
})
@Getter
@Setter
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 匿名设备标识（前端 localStorage 生成，无登录体系的用户锚点） */
    @Column(nullable = false, length = 64)
    private String deviceId;

    /** Push Service 投递地址（endpoint，唯一） */
    @Column(nullable = false, length = 500, unique = true)
    private String endpoint;

    /** 应用服务器公钥（payload 加密用，浏览器下发） */
    @Column(nullable = false, length = 128)
    private String p256dh;

    /** 认证密钥（payload 加密用，浏览器下发） */
    @Column(nullable = false, length = 64)
    private String auth;

    @Column(nullable = false)
    private long createdAt;
}
