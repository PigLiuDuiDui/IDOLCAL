package com.example.bim.api.entity;

/**
 * 推送任务状态机：
 * PENDING → PROCESSING → SUCCESS
 *             ↓ 失败
 *           FAILED → RETRY（重试）→ PROCESSING → SUCCESS
 * 状态转移由 PushTaskService 统一处理（CAS 更新，避免重复执行）。
 */
public enum PushTaskStatus {
    /** 已创建，等待到达触发时刻 */
    PENDING,
    /** 正在发送（Quartz 触发或兜底扫描认领） */
    PROCESSING,
    /** 发送成功（全部目标订阅完成投递） */
    SUCCESS,
    /** 最终失败（超过最大重试次数） */
    FAILED,
    /** 发送失败，等待重试（next_retry_at 到期后重新进入 PROCESSING） */
    RETRY
}
