package com.example.bim.api.service;

import com.example.bim.api.entity.Event;
import com.example.bim.api.entity.PushSchedule;
import com.example.bim.api.entity.PushTask;
import com.example.bim.api.entity.PushTaskStatus;
import com.example.bim.api.repository.EventRepository;
import com.example.bim.api.repository.PushScheduleRepository;
import com.example.bim.api.repository.PushTaskRepository;
import com.example.bim.api.schedule.PushTaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推送调度 + Fan-out 执行（万人级推送核心）：
 * - 调度模型：一个 (eventId, offsetMinutes) 一个 PushSchedule + 一个 Quartz Job，
 *   Quartz Job 数量 = 提醒时间点数量（远小于用户数）；触发时批量处理该调度下所有设备任务。
 * - 执行：CAS 认领调度 → 一次查出调度下所有可投递任务 → 内存分批（fanout-batch-size）→
 *   有界并发发送（WebPushService 线程池）→ 任务状态与投递日志批量落库（saveAll）。
 * - 状态机（调度级，CAS）：
 *   认领：PENDING | RETRY → PROCESSING（attemptId 递增，收尾校验防旧尝试倒灌）
 *   收尾：还有任务在重试 → RETRY（调度级退避）；否则 SUCCESS
 *   超时恢复：PROCESSING → PENDING（仅重置，由下轮扫描补跑，不立即重发）
 */
@Service
public class PushTaskService {

    private static final Logger log = LoggerFactory.getLogger(PushTaskService.class);

    /** 任务最大重试次数（超过后标记 FAILED，不再发送）；调度级重试无上限（由任务驱动） */
    private static final int MAX_RETRY = 3;
    /** 重试退避（第 1/2/3 次失败后等待 1/5/30 分钟） */
    private static final long[] RETRY_DELAYS = {60_000L, 300_000L, 1_800_000L};
    /** 未完成任务状态（孤儿调度判定 / 监控收件人数） */
    private static final List<PushTaskStatus> ACTIVE = List.of(
            PushTaskStatus.PENDING, PushTaskStatus.PROCESSING, PushTaskStatus.RETRY);

    /** PROCESSING 超时阈值：进程崩溃后卡住的调度重置重跑（可配置） */
    private final long processingTimeoutMs;
    /** Fan-out 内存分批大小：一次查询出调度下全部任务，按此分批发送（可配置） */
    private final int fanoutBatchSize;

    private final PushTaskRepository tasks;
    private final PushScheduleRepository schedules;
    private final EventRepository events;
    private final WebPushService webPush;
    private final PushTaskScheduler scheduler;
    private final TransactionTemplate tx;

    public PushTaskService(PushTaskRepository tasks, PushScheduleRepository schedules,
                           EventRepository events, WebPushService webPush,
                           PushTaskScheduler scheduler, TransactionTemplate tx,
                           @Value("${idolcal.push.processing-timeout-ms:600000}") long processingTimeoutMs,
                           @Value("${idolcal.push.fanout-batch-size:500}") int fanoutBatchSize) {
        this.tasks = tasks;
        this.schedules = schedules;
        this.events = events;
        this.webPush = webPush;
        this.scheduler = scheduler;
        this.tx = tx;
        this.processingTimeoutMs = processingTimeoutMs;
        this.fanoutBatchSize = Math.max(1, fanoutBatchSize);
    }

    // ---- 提醒同步（全量替换该设备的推送任务） ----

    /**
     * 全量替换设备提醒：前端每次变更后上传完整列表。
     * 同一 (eventId, offsetMinutes) 幂等复用调度（唯一约束兜底并发），
     * 设备任务挂到调度下；设备取消后调度下无其他任务时删除调度 + Quartz Job。
     */
    public void syncReminders(String deviceId, List<ReminderItem> items) {
        long now = System.currentTimeMillis();
        // 1. 记录旧任务关联的调度，删除该设备全部旧任务
        Set<Long> affected = new HashSet<>();
        tx.executeWithoutResult(status -> {
            for (PushTask t : tasks.findByDeviceId(deviceId)) {
                if (t.getScheduleId() != null) affected.add(t.getScheduleId());
            }
            tasks.deleteByDeviceId(deviceId);
        });
        // 2. 按 (eventId, offsetMinutes) 幂等 upsert 调度，新任务挂到调度下
        Set<Long> touched = new HashSet<>();
        int created = 0;
        for (ReminderItem item : items) {
            Event event = events.findById(item.eventId()).orElse(null);
            if (event == null || event.getStartAtUtc() == null) {
                log.info("[push] 跳过提醒：活动不存在或无确定时刻 eventId={}", item.eventId());
                continue; // 活动不存在 / 全天无时刻，不建任务
            }
            long triggerAt = event.getStartAtUtc() - item.offsetMinutes() * 60_000L;
            if (triggerAt <= now) continue; // 已过期不补推
            PushSchedule schedule = ensureSchedule(item.eventId(), item.offsetMinutes(), triggerAt);
            touched.add(schedule.getId());
            PushTask task = new PushTask();
            task.setDeviceId(deviceId);
            task.setEventId(item.eventId());
            task.setOffsetMinutes(item.offsetMinutes());
            task.setTriggerAt(triggerAt);
            task.setScheduleId(schedule.getId());
            task.setStatus(PushTaskStatus.PENDING);
            task.setCreatedAt(now);
            tasks.save(task);
            created++;
        }
        // 3. 清理孤儿调度：设备取消后调度下已无未完成任务 → 删除调度 + Quartz Job
        for (Long sid : affected) {
            if (touched.contains(sid)) continue;
            cleanupOrphanSchedule(sid);
        }
        log.info("[push] 提醒已同步 deviceId={} total={} created={}", deviceId, items.size(), created);
    }

    /** 幂等获取调度：不存在则创建并注册 Quartz Job（唯一约束兜底并发，撞冲突时回读已有） */
    private PushSchedule ensureSchedule(String eventId, int offsetMinutes, long triggerAt) {
        PushSchedule existing = schedules.findFirstByEventIdAndOffsetMinutes(eventId, offsetMinutes).orElse(null);
        if (existing != null) return existing;
        try {
            PushSchedule created = new PushSchedule();
            created.setEventId(eventId);
            created.setOffsetMinutes(offsetMinutes);
            created.setTriggerAt(triggerAt);
            created.setStatus(PushTaskStatus.PENDING);
            created.setCreatedAt(System.currentTimeMillis());
            schedules.save(created);
            scheduler.schedule(created);
            log.info("[push] 新建调度 {}（{}，提前 {} 分钟，触发 {}）",
                    created.getId(), eventId, offsetMinutes, new Date(triggerAt));
            return created;
        } catch (Exception e) {
            PushSchedule race = schedules.findFirstByEventIdAndOffsetMinutes(eventId, offsetMinutes).orElse(null);
            if (race != null) return race;
            throw e;
        }
    }

    /** 调度下无任何未完成任务 → 删除调度 + Quartz Job（已完结的调度保留为历史记录） */
    private void cleanupOrphanSchedule(Long scheduleId) {
        if (scheduleId == null) return;
        if (tasks.countByScheduleIdAndStatusIn(scheduleId, ACTIVE) > 0) return;
        PushSchedule s = schedules.findById(scheduleId).orElse(null);
        if (s == null) return;
        if (s.getStatus() == PushTaskStatus.SUCCESS || s.getStatus() == PushTaskStatus.FAILED) return;
        scheduler.unschedule(s);
        schedules.delete(s);
        log.info("[push] 调度 {} 已无未完成任务，删除（{}，提前 {} 分钟）", scheduleId, s.getEventId(), s.getOffsetMinutes());
    }

    // ---- Fan-out 执行（Quartz 触发 + 兜底扫描共用入口） ----

    /**
     * 处理调度：CAS 认领 → 查询调度下所有可投递任务 → 分批发送 → 收尾。
     * 认领成功才执行，Quartz 与兜底扫描并发命中时只有一个执行者。
     */
    public void processSchedule(Long scheduleId) {
        PushSchedule schedule = schedules.findById(scheduleId).orElse(null);
        if (schedule == null) return;
        Long attemptId = claim(schedule);
        if (attemptId == null) return; // 已被并发认领，或状态已非可认领态

        Event event = events.findById(schedule.getEventId()).orElse(null);
        if (event == null) {
            // 活动已删除：调度直接完成（无内容可推）
            finishSchedule(scheduleId, attemptId, true);
            return;
        }
        byte[] payload = webPush.payloadFor(event);
        List<PushTask> due = tasks.findDueBySchedule(scheduleId,
                PushTaskStatus.PENDING, PushTaskStatus.RETRY, System.currentTimeMillis());
        for (int i = 0; i < due.size(); i += fanoutBatchSize) {
            // 批次前校验：调度被超时重置后新尝试已接手时，放弃后续批次，避免继续重复发送
            if (attemptStale(scheduleId, attemptId)) {
                log.info("[push] 调度 {} 已被新尝试接手，放弃后续批次（attempt={}）", scheduleId, attemptId);
                break;
            }
            List<PushTask> batch = due.subList(i, Math.min(i + fanoutBatchSize, due.size()));
            List<WebPushService.TaskSendResult> results = webPush.sendBatch(batch, payload);
            // 发送期间调度可能已被新尝试认领（旧 Worker 迟到结果）：必须丢弃，防止覆盖新尝试状态
            if (attemptStale(scheduleId, attemptId)) {
                log.info("[push] 调度 {} 本批发送期间已被新尝试接手，迟到结果丢弃（attempt={}）", scheduleId, attemptId);
                break;
            }
            saveTaskResults(batch, results);
        }
        // 收尾：还有任务在重试（本轮发送失败的）→ 调度 RETRY，等 retryDue 补跑；否则 SUCCESS
        boolean allDone = tasks.countByScheduleIdAndStatusIn(scheduleId, List.of(PushTaskStatus.RETRY)) == 0;
        finishSchedule(scheduleId, attemptId, allDone);
        log.info("[push] 调度 {} 处理完成（{}，提前 {} 分钟）：任务 {} 条{}",
                scheduleId, schedule.getEventId(), schedule.getOffsetMinutes(), due.size(),
                allDone ? "" : "，部分任务进入重试");
    }

    /**
     * CAS 认领调度：PENDING | RETRY → PROCESSING（短事务，避免长事务持锁）。
     * 认领成功返回本次 attemptId（收尾时校验，防旧尝试迟到结果倒灌）。
     */
    private Long claim(PushSchedule schedule) {
        return tx.execute(status -> {
            if (schedules.transition(schedule.getId(),
                    List.of(PushTaskStatus.PENDING, PushTaskStatus.RETRY), PushTaskStatus.PROCESSING) == 0) {
                return null;
            }
            PushSchedule s = schedules.findById(schedule.getId()).orElseThrow();
            long attempt = (s.getAttemptId() == null ? 0 : s.getAttemptId()) + 1;
            s.setAttemptId(attempt);
            s.setProcessedAt(System.currentTimeMillis());
            schedules.save(s);
            return attempt;
        });
    }

    /**
     * 批次任务结果批量落库：内存对象更新 + saveAll（替代每任务一次 DB 写）。
     * 任务级状态机：SUCCESS（至少一台送达 / 订阅全失效）/ RETRY（未超限）/ FAILED（超限）。
     */
    private void saveTaskResults(List<PushTask> batch, List<WebPushService.TaskSendResult> results) {
        Map<Long, PushTask> byId = new HashMap<>();
        for (PushTask t : batch) byId.put(t.getId(), t);
        long now = System.currentTimeMillis();
        for (WebPushService.TaskSendResult r : results) {
            PushTask t = byId.get(r.taskId());
            if (t == null) continue;
            WebPushService.SendOutcome outcome = r.outcome();
            if (outcome.anySuccess() || outcome.allExpired()) {
                t.setStatus(PushTaskStatus.SUCCESS);
                t.setSentAt(now);
                t.setErrorMessage(outcome.message());
            } else if (t.getRetryCount() < MAX_RETRY) {
                t.setStatus(PushTaskStatus.RETRY);
                t.setRetryCount(t.getRetryCount() + 1);
                long delay = RETRY_DELAYS[Math.min(t.getRetryCount() - 1, RETRY_DELAYS.length - 1)];
                t.setNextRetryAt(now + delay);
                t.setErrorMessage(outcome.message());
                log.warn("[push] 任务 {} 第 {} 次发送失败，{} 后重试：{}",
                        t.getId(), t.getRetryCount(), delay / 1000 + "s", outcome.message());
            } else {
                t.setStatus(PushTaskStatus.FAILED);
                t.setErrorMessage(outcome.message());
                log.error("[push] 任务 {} 超过最大重试次数，标记失败：{}", t.getId(), outcome.message());
            }
        }
        tasks.saveAll(batch);
    }

    /**
     * 调度收尾：全部任务终态 → SUCCESS；仍有任务在重试 → 调度 RETRY（调度级退避）。
     * attemptId 不匹配或状态已离开 PROCESSING（调度被超时重置后新尝试已接手）则丢弃旧尝试结果。
     */
    private void finishSchedule(long scheduleId, long attemptId, boolean allDone) {
        tx.executeWithoutResult(status -> {
            PushSchedule s = schedules.findById(scheduleId).orElse(null);
            if (s == null) return;
            if (s.getAttemptId() == null || s.getAttemptId() != attemptId
                    || s.getStatus() != PushTaskStatus.PROCESSING) {
                log.info("[push] 调度 {} 旧尝试 {} 迟到结果已忽略（当前 attempt={}，status={}）",
                        scheduleId, attemptId, s.getAttemptId(), s.getStatus());
                return;
            }
            long now = System.currentTimeMillis();
            if (allDone) {
                s.setStatus(PushTaskStatus.SUCCESS);
                s.setFinishedAt(now);
            } else {
                s.setStatus(PushTaskStatus.RETRY);
                s.setRetryCount(s.getRetryCount() + 1);
                long delay = RETRY_DELAYS[Math.min(s.getRetryCount() - 1, RETRY_DELAYS.length - 1)];
                s.setNextRetryAt(now + delay);
                log.warn("[push] 调度 {} 存在待重试任务，{} 后重试", scheduleId, delay / 1000 + "s");
            }
            schedules.save(s);
        });
    }

    /**
     * 当前调度是否仍属于本次尝试：attemptId 一致且状态为 PROCESSING 才算有效。
     * 调度被超时重置（状态 → PENDING）或新尝试认领（attemptId +1）后，旧尝试的所有结果必须丢弃。
     */
    private boolean attemptStale(long scheduleId, long attemptId) {
        PushSchedule s = schedules.findById(scheduleId).orElse(null);
        return s == null || s.getAttemptId() == null || s.getAttemptId() != attemptId
                || s.getStatus() != PushTaskStatus.PROCESSING;
    }

    // ---- 兜底恢复（调度器每分钟调用） ----

    /** PENDING 到期未触发（Quartz 丢失 / 重启）→ 立即补跑 */
    public void recoverPending() {
        List<PushSchedule> due = schedules.findByStatusAndTriggerAtLessThanEqual(
                PushTaskStatus.PENDING, System.currentTimeMillis());
        for (PushSchedule s : due) processSchedule(s.getId());
    }

    /** RETRY 到达重试时间 → 重新 Fan-out（CAS 认领支持 RETRY → PROCESSING） */
    public void retryDue() {
        List<PushSchedule> due = schedules.findByStatusAndNextRetryAtLessThanEqual(
                PushTaskStatus.RETRY, System.currentTimeMillis());
        for (PushSchedule s : due) processSchedule(s.getId());
    }

    /**
     * PROCESSING 卡住超过阈值（进程崩溃 / 长时间卡顿）→ 重置为 PENDING。
     * 注意：只重置、不立即重跑——若原尝试仍在发送中（Push Service 慢响应），
     * 立即重跑会造成重复推送；重置后由下一轮 recoverPending 补跑（最多延迟一个扫描周期）。
     * 重复发送本身无法完全消除（Web Push 无 Exactly Once），
     * 已通过调度 attemptId 防状态倒灌 + 前端通知 tag 去重控制影响。
     */
    public void recoverStale() {
        long cutoff = System.currentTimeMillis() - processingTimeoutMs;
        List<PushSchedule> stuck = schedules.findByStatusAndProcessedAtLessThanEqual(
                PushTaskStatus.PROCESSING, cutoff);
        for (PushSchedule s : stuck) {
            Boolean reset = tx.execute(status ->
                    schedules.resetStatus(s.getId(), PushTaskStatus.PROCESSING, PushTaskStatus.PENDING) > 0);
            if (Boolean.TRUE.equals(reset)) {
                log.warn("[push] 调度 {} 超时未完成（attempt={}），已重置为 PENDING，等待下轮扫描补跑",
                        s.getId(), s.getAttemptId());
            }
        }
    }

    /** 提醒同步条目（前端上传：活动 id + 开始前分钟数） */
    public record ReminderItem(String eventId, int offsetMinutes) {
    }
}
