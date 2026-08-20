package com.example.bim.api.repository;

import com.example.bim.api.entity.PushTask;
import com.example.bim.api.entity.PushTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushTaskRepository extends JpaRepository<PushTask, Long> {

    /** 兜底扫描：到期且未开始处理的任务（Quartz 丢失 / 重启恢复） */
    List<PushTask> findByStatusAndTriggerAtLessThanEqual(PushTaskStatus status, long now);

    /** 重试扫描：到达重试时间的任务 */
    List<PushTask> findByStatusAndNextRetryAtLessThanEqual(PushTaskStatus status, long now);

    /** 超时恢复：PROCESSING 卡住超过阈值（进程崩溃）的任务 */
    List<PushTask> findByStatusAndProcessedAtLessThanEqual(PushTaskStatus status, long cutoff);

    List<PushTask> findByDeviceId(String deviceId);

    Optional<PushTask> findFirstByDeviceIdAndEventIdAndStatusIn(String deviceId, String eventId, List<PushTaskStatus> statuses);

    void deleteByDeviceId(String deviceId);

    /** 任务状态分布（后台监控：PENDING / PROCESSING / SUCCESS / FAILED / RETRY） */
    @Query("select t.status, count(t) from PushTask t group by t.status")
    List<Object[]> countByStatus();

    /** 未来窗口内未完成的任务（后台「即将触发」监控，按状态集 + 触发时刻） */
    List<PushTask> findByStatusInAndTriggerAtBetween(Collection<PushTaskStatus> statuses, long from, long to);

    /**
     * CAS 认领：仅当任务处于可认领状态时才推进，避免并发重复发送。
     * 可认领状态必须包含 PENDING 与 RETRY：Quartz 首次触发认领 PENDING，
     * 重试任务（retryDue）认领 RETRY；只允许 PENDING → PROCESSING
     * 会导致 RETRY 任务永远无法重试。
     */
    @Modifying
    @Query("update PushTask t set t.status = :to where t.id = :id and t.status in :from")
    int transition(@Param("id") long id, @Param("from") Collection<PushTaskStatus> from, @Param("to") PushTaskStatus to);

    /** CAS 重置：仅当任务仍为指定状态时恢复为 PENDING（超时任务重置重跑） */
    @Modifying
    @Query("update PushTask t set t.status = :to where t.id = :id and t.status = :from")
    int resetStatus(@Param("id") long id, @Param("from") PushTaskStatus from, @Param("to") PushTaskStatus to);

    // ---- Fan-out（调度触发时按调度批量取任务） ----

    /**
     * 调度下所有可投递任务：PENDING（首次触发），或 RETRY 已到重试时间（重试轮）。
     * 一次查询全部（内存分批发送），避免处理过程中状态变化破坏分页游标语义。
     */
    @Query("""
            select t from PushTask t
            where (t.scheduleId = :scheduleId and t.status = :pending)
               or (t.scheduleId = :scheduleId and t.status = :retry and t.nextRetryAt <= :now)
            order by t.id
            """)
    List<PushTask> findDueBySchedule(@Param("scheduleId") Long scheduleId,
                                     @Param("pending") PushTaskStatus pending,
                                     @Param("retry") PushTaskStatus retry,
                                     @Param("now") long now);

    /** 调度下处于指定状态的任务数（孤儿调度判定 / 调度收尾 / 监控收件人数） */
    long countByScheduleIdAndStatusIn(Long scheduleId, Collection<PushTaskStatus> statuses);
}
