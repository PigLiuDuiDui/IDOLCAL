package com.example.bim.api.repository;

import com.example.bim.api.entity.PushDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
