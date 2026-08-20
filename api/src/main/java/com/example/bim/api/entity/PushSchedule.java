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
 * 推送调度（Fan-out 模型：Quartz 调度单元）。
 * 一个 (eventId, offsetMinutes) 对应一个调度、一个 Quartz Job；
 * 触发时按批次 Fan-out 处理该调度下所有设备任务（push_tasks.schedule_id）。
 * 万人级场景下 Quartz Job 数量从「用户数 × 提醒数」降为「提醒时间点数量」，
 * 一个活动一次推送（8000 用户）只有 1 个 Job。
 */
@Entity
@Table(name = "push_schedules", uniqueConstraints = {
        @UniqueConstraint(name = "uk_schedule_event_offset", columnNames = {"event_id", "offset_minutes"})
}, indexes = {
        @Index(name = "idx_schedules_status_trigger", columnList = "status, trigger_at"),
        @Index(name = "idx_schedules_status_retry", columnList = "status, next_retry_at"),
        @Index(name = "idx_schedules_status_processed", columnList = "status, processed_at")
})
@Getter
@Setter
public class PushSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 活动 id（events.id，触发时实时读取标题 / 时间组装通知） */
    @Column(nullable = false, length = 32, name = "event_id")
    private String eventId;

    /** 活动开始前分钟数（0 = 开始时） */
    @Column(nullable = false, name = "offset_minutes")
    private int offsetMinutes;

    /** 触发时刻（UTC epoch millis，后端按 event + timezone + offset 计算） */
    @Column(nullable = false, name = "trigger_at")
    private long triggerAt;

    /** 调度状态（PENDING / PROCESSING / SUCCESS / FAILED / RETRY） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PushTaskStatus status;

    /** 进入 PROCESSING 的时刻（超时恢复用：进程崩溃后卡住的调度会重置重跑） */
    @Column(name = "processed_at")
    private Long processedAt;

    /**
     * 执行尝试编号（每次认领 +1）。调度收尾时校验 attemptId：
     * 调度被超时重置重跑后，旧尝试的迟到结果不会覆盖新尝试的状态（防状态倒灌）。
     */
    @Column(name = "attempt_id")
    private Long attemptId;

    /** 已重试次数（调度级退避；只要还有任务在重试就不设上限，任务级有限制） */
    @Column(nullable = false, name = "retry_count")
    private int retryCount;

    /** 下次重试时刻（状态为 RETRY 时有效） */
    @Column(name = "next_retry_at")
    private Long nextRetryAt;

    /** 全部任务完成时刻 */
    @Column(name = "finished_at")
    private Long finishedAt;

    @Column(nullable = false, name = "created_at")
    private long createdAt;
}
