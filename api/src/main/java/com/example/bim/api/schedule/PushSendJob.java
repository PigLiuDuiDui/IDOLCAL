package com.example.bim.api.schedule;

import com.example.bim.api.service.PushTaskService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Quartz 触发 Job：从 JobDataMap 取调度 id，委托 PushTaskService 对该调度 Fan-out。
 * 一个调度对应一个活动提醒时间点（一个 Quartz Job），到点后由服务层
 * 批量处理调度下所有设备任务；状态流转 CAS 保证与兜底扫描并发时只发送一次。
 */
public class PushSendJob implements Job {

    @Autowired
    private PushTaskService pushTaskService;

    @Override
    public void execute(JobExecutionContext context) {
        long scheduleId = context.getMergedJobDataMap().getLong("scheduleId");
        pushTaskService.processSchedule(scheduleId);
    }
}
