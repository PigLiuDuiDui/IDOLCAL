package com.example.bim.api.config;

import com.example.bim.api.repository.PushDeliveryLogRepository;
import com.example.bim.api.service.PushTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推送兜底调度：
 * 1. recoveryScan（每分钟）——任务恢复：
 *    - recoverPending  Quartz 丢失 / 重启后未触发的到期任务补跑
 *    - retryDue        发送失败进入 RETRY 且到达重试时间的任务重发
 *    - recoverStale    进程崩溃后卡在 PROCESSING 的任务重置重跑（不立即重发）
 *    状态流转统一走 CAS，与 Quartz 并发触发不会重复发送。
 * 2. cleanupLogs（每日凌晨）——投递日志保留期清理（控制 push_delivery_logs 增长）。
 */
@Component
@EnableScheduling
public class PushScheduler {

    private static final Logger log = LoggerFactory.getLogger(PushScheduler.class);

    private final PushTaskService pushTaskService;
    private final PushDeliveryLogRepository deliveryLogs;
    private final long logRetentionMs;

    public PushScheduler(PushTaskService pushTaskService,
                         PushDeliveryLogRepository deliveryLogs,
                         @Value("${idolcal.push.delivery-log-retention-days:90}") long retentionDays) {
        this.pushTaskService = pushTaskService;
        this.deliveryLogs = deliveryLogs;
        this.logRetentionMs = Math.max(1, retentionDays) * 86_400_000L;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void recoveryScan() {
        try {
            pushTaskService.recoverPending();
            pushTaskService.retryDue();
            pushTaskService.recoverStale();
        } catch (Exception e) {
            // 调度任务异常不得中断后续周期
            log.warn("[push] 调度扫描异常：{}", e.getMessage());
        }
    }

    /** 每天凌晨清理保留期之前的投递日志（万人级每天数万条，必须控制表增长） */
    @Scheduled(cron = "0 30 3 * * *")
    public void cleanupLogs() {
        long cutoff = System.currentTimeMillis() - logRetentionMs;
        try {
            int removed = deliveryLogs.deleteBySentAtBefore(cutoff);
            if (removed > 0) {
                log.info("[push] 已清理 {} 天前的投递日志 {} 条", logRetentionMs / 86_400_000L, removed);
            }
        } catch (Exception e) {
            log.warn("[push] 投递日志清理异常：{}", e.getMessage());
        }
    }
}
