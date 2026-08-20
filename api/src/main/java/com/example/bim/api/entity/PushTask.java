package com.example.bim.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * 推送任务（Web Push 调度单元）
 * trigger_at 由后端计算：event.start_at_utc - offset_minutes（前端只传 eventId + offsetMinutes）；
 * 触发时服务端实时读取活动数据组装通知内容（活动改期 / 删除自动跟随）。
 */
@Entity
@Table(name = "push_tasks", uniqueConstraints = {
        /** 同一设备在同一调度下只能存在一个任务（syncReminders 全量替换的并发兜底，防止重复推送） */
        @UniqueConstraint(name = "uk_push_tasks_schedule_device",
                columnNames = {"schedule_id", "device_id"})
}, indexes = {
        @Index(name = "idx_push_tasks_status_trigger", columnList = "status, trigger_at"),
        @Index(name = "idx_push_tasks_status_retry", columnList = "status, next_retry_at"),
        @Index(name = "idx_push_tasks_device", columnList = "device_id"),
        @Index(name = "idx_push_tasks_schedule", columnList = "schedule_id")
})
@Getter
@Setter
public class PushTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 匿名设备标识（与 user_devices.device_id 对应，未来绑定用户） */
    @Column(nullable = false, length = 64)
    private String deviceId;

    /** 活动 id（events.id，推送时实时读取标题 / 时间） */
    @Column(nullable = false, length = 32)
    private String eventId;

    /**
     * 所属调度 id（push_schedules.id，Fan-out 执行单元）。
     * 同一 (eventId, offsetMinutes) 的所有设备任务共享一个调度；
     * Quartz 只按调度注册 Job，触发时批量处理本调度下的任务。
     */
    @Column(name = "schedule_id")
    private Long scheduleId;

    /** 活动开始前分钟数（0 = 开始时） */
    @Column(nullable = false)
    private int offsetMinutes;

    /** 触发时刻（UTC epoch millis，后端按 event + timezone + offset 计算） */
    @Column(nullable = false, name = "trigger_at")
    private long triggerAt;

    /** 任务状态（PENDING / PROCESSING / SUCCESS / FAILED / RETRY） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PushTaskStatus status;

    /** 进入 PROCESSING 的时刻（超时恢复用：进程崩溃后卡住的任务会重置重跑） */
    @Column(name = "processed_at")
    private Long processedAt;

    /**
     * 执行尝试编号（每次认领 +1）。发送结果落库时校验 attemptId：
     * 任务被超时重置重跑后，旧尝试的迟到结果不会覆盖新尝试的状态（防状态倒灌）。
     */
    @Column(name = "attempt_id")
    private Long attemptId;

    /** 已重试次数 */
    @Column(nullable = false)
    private int retryCount;

    /** 下次重试时刻（状态为 RETRY 时有效） */
    @Column(name = "next_retry_at")
    private Long nextRetryAt;

    /** 成功发送时刻 */
    @Column(name = "sent_at")
    private Long sentAt;

    /** 最近一次失败原因 */
    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, name = "created_at")
    private long createdAt;
}
