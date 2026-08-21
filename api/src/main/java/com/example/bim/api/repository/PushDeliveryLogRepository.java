package com.example.bim.api.repository;

import com.example.bim.api.entity.PushDeliveryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PushDeliveryLogRepository extends JpaRepository<PushDeliveryLog, Long> {

    /** 统计某时刻之后的投递结果分布（今日发送 / 成功 / 失败） */
    @Query("""
            select l.result, count(l) from PushDeliveryLog l
            where l.sentAt >= :from group by l.result
            """)
    List<Object[]> countByResultSince(@Param("from") long from);

    List<PushDeliveryLog> findTop50ByTaskIdOrderBySentAtDesc(Long taskId);

    /** 批量清理保留期之前的日志（每日定时任务调用，控制日志表增长） */
    @Modifying
    @Query("delete from PushDeliveryLog l where l.sentAt < :cutoff")
    int deleteBySentAtBefore(@Param("cutoff") long cutoff);

    // ---- 后台管理查询（只读） ----

    /** 按结果筛选的投递日志分页（推送记录页；result 为空则全部） */
    @Query("""
            select l from PushDeliveryLog l
            where (:result is null or l.result = :result)
              and (:q is null or l.deviceId like concat('%', :q, '%') or l.endpoint like concat('%', :q, '%'))
            order by l.sentAt desc
            """)
    Page<PushDeliveryLog> search(@Param("result") String result, @Param("q") String q, Pageable pageable);

    /** 按天聚合投递结果（后台 Dashboard 趋势：day 为 UTC 自然日 epoch day） */
    @Query("""
            select l.sentAt / 86400000 as day, l.result, count(l)
            from PushDeliveryLog l
            where l.sentAt >= :from
            group by l.sentAt / 86400000, l.result
            order by day
            """)
    List<Object[]> countByDaySince(@Param("from") long from);

    /** 调度时间线样本：调度下所有任务的投递日志按时间升序（上限 500 条，批次粒度展示用） */
    List<PushDeliveryLog> findTop500ByTaskIdInOrderBySentAtAsc(Collection<Long> taskIds);
}
