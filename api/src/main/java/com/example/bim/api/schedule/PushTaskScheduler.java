package com.example.bim.api.schedule;

import com.example.bim.api.entity.PushSchedule;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Quartz 精确调度封装：每个推送调度（PushSchedule）注册一个一次性 Job，到点触发 PushSendJob；
 * 触发时由 PushTaskService Fan-out 处理该调度下所有设备任务。
 * 万人级场景下 Job 数量 = 提醒时间点数量（一个活动一次推送只有 1 个 Job），
 * 而不是「用户数 × 提醒数」。调度状态落库（push_schedules），
 * Quartz 丢失 / 重启后由兜底扫描补跑。
 */
@Component
public class PushTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(PushTaskScheduler.class);

    private final Scheduler scheduler;

    public PushTaskScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /** 注册调度（幂等：已存在则不重复注册） */
    public void schedule(PushSchedule schedule) {
        try {
            JobKey key = jobKey(schedule.getId());
            if (scheduler.checkExists(key)) return;
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("push-trigger-" + schedule.getId())
                    .startAt(new Date(schedule.getTriggerAt()))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow()) // 错过触发立即补跑
                    .build();
            JobDetail detail = JobBuilder.newJob(PushSendJob.class)
                    .withIdentity(key)
                    .usingJobData("scheduleId", schedule.getId())
                    .build();
            scheduler.scheduleJob(detail, trigger);
            log.info("[push] 调度 {} 已注册 Quartz 触发（{}）", schedule.getId(), new Date(schedule.getTriggerAt()));
        } catch (SchedulerException e) {
            log.warn("[push] 调度 {} Quartz 注册失败（兜底扫描会补跑）：{}", schedule.getId(), e.getMessage());
        }
    }

    /** 删除调度注册（调度下无任务时清理，避免孤儿 Job 空转） */
    public void unschedule(PushSchedule schedule) {
        try {
            scheduler.deleteJob(jobKey(schedule.getId()));
        } catch (SchedulerException e) {
            log.warn("[push] 调度 {} Quartz 删除失败：{}", schedule.getId(), e.getMessage());
        }
    }

    private JobKey jobKey(long scheduleId) {
        return JobKey.jobKey("push-schedule-" + scheduleId, "push");
    }
}
