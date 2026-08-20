package com.example.bim.api.repository;

import com.example.bim.api.entity.PushSchedule;
import com.example.bim.api.entity.PushTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushScheduleRepository extends JpaRepository<PushSchedule, Long> {

    /** 同一活动 + 提前量的调度唯一（幂等 upsert 用，配合唯一约束兜底并发） */
    Optional<PushSchedule> findFirstByEventIdAndOffsetMinutes(String eventId, int offsetMinutes);

    /** 兜底扫描：到期且未开始处理的调度（Quartz 丢失 / 重启恢复） */
    List<PushSchedule> findByStatusAndTriggerAtLessThanEqual(PushTaskStatus status, long now);

    /** 重试扫描：到达重试时间的调度 */
    List<PushSchedule> findByStatusAndNextRetryAtLessThanEqual(PushTaskStatus status, long now);

    /** 超时恢复：PROCESSING 卡住超过阈值（进程崩溃）的调度 */
    List<PushSchedule> findByStatusAndProcessedAtLessThanEqual(PushTaskStatus status, long cutoff);

    /** 调度状态分布（后台监控：PENDING / PROCESSING / SUCCESS / FAILED / RETRY） */
    @Query("select s.status, count(s) from PushSchedule s group by s.status")
    List<Object[]> countByStatus();

    /** 未来窗口内未完成的调度（后台「即将触发」监控） */
    List<PushSchedule> findByStatusInAndTriggerAtBetween(Collection<PushTaskStatus> statuses, long from, long to);

    /**
     * CAS 认领：仅当调度处于可认领状态时才推进，避免并发重复发送。
     * 可认领状态包含 PENDING 与 RETRY：Quartz 首次触发认领 PENDING，
     * 重试（retryDue）认领 RETRY；只允许 PENDING → PROCESSING
     * 会导致 RETRY 调度永远无法重试。
     */
    @Modifying
    @Query("update PushSchedule s set s.status = :to where s.id = :id and s.status in :from")
    int transition(@Param("id") long id, @Param("from") Collection<PushTaskStatus> from, @Param("to") PushTaskStatus to);

    /** CAS 重置：仅当调度仍为指定状态时恢复为 PENDING（超时调度重置重跑） */
    @Modifying
    @Query("update PushSchedule s set s.status = :to where s.id = :id and s.status = :from")
    int resetStatus(@Param("id") long id, @Param("from") PushTaskStatus from, @Param("to") PushTaskStatus to);
}
