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
 * 推送投递日志（每次发送一条，后台统计 / 排查用）
 * result: SUCCESS / FAILED / EXPIRED（订阅失效，410/404）
 */
@Entity
@Table(name = "push_delivery_logs", indexes = {
        @Index(name = "idx_delivery_sent_at", columnList = "sent_at"),
        @Index(name = "idx_delivery_task", columnList = "task_id"),
        // 后台统计（countByResultSince）按 sent_at + result 查询
        @Index(name = "idx_delivery_result_sent", columnList = "result, sent_at")
})
@Getter
@Setter
public class PushDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 推送任务 id（push_tasks.id，可空：测试通知等无任务来源） */
    @Column(name = "task_id")
    private Long taskId;

    /** 目标设备标识 */
    @Column(nullable = false, length = 64)
    private String deviceId;

    /** 投递目标 Push Service endpoint（截断保存，避免超长） */
    @Column(nullable = false, length = 200)
    private String endpoint;

    /** 发送时刻（UTC epoch millis） */
    @Column(nullable = false, name = "sent_at")
    private long sentAt;

    /** Push Service HTTP 状态（0 = 网络异常未收到响应） */
    @Column(nullable = false, name = "http_status")
    private int httpStatus;

    /** 结果：SUCCESS / FAILED / EXPIRED */
    @Column(nullable = false, length = 16)
    private String result;

    /** 失败原因（网络异常信息；成功为空） */
    @Column(length = 500)
    private String errorMessage;
}
