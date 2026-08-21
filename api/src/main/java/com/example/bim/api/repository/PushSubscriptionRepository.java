package com.example.bim.api.repository;

import com.example.bim.api.entity.PushSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByDeviceId(String deviceId);

    /** 批量按设备查订阅（Fan-out 批次内一次查询，避免每任务一次 N+1 查询） */
    List<PushSubscription> findByDeviceIdIn(Collection<String> deviceIds);

    void deleteByDeviceId(String deviceId);

    /** 设备订阅分页（后台：订阅管理） */
    Page<PushSubscription> findByDeviceId(String deviceId, Pageable pageable);

    /** 启用推送的设备数（后台：Push Enabled，按设备去重） */
    @Query("select count(distinct s.deviceId) from PushSubscription s")
    long countDistinctDeviceId();
}
