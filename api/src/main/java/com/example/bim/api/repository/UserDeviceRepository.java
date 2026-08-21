package com.example.bim.api.repository;

import com.example.bim.api.entity.UserDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByDeviceId(String deviceId);

    List<UserDevice> findByUserId(Long userId);

    void deleteByDeviceId(String deviceId);

    // ---- 后台管理查询（只读） ----

    /** 最近活跃设备数（后台 Dashboard：Active Users） */
    long countByLastActiveAtGreaterThanEqual(long since);

    /** 新注册设备数（后台 Dashboard：New Users） */
    long countByCreatedAtGreaterThanEqual(long since);

    /** 后台设备搜索（deviceId 模糊） */
    @Query("select d from UserDevice d where :q is null or d.deviceId like concat('%', :q, '%') order by d.lastActiveAt desc")
    Page<UserDevice> search(@Param("q") String q, Pageable pageable);
}
