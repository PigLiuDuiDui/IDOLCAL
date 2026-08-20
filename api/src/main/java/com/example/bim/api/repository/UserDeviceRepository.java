package com.example.bim.api.repository;

import com.example.bim.api.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByDeviceId(String deviceId);

    List<UserDevice> findByUserId(Long userId);

    void deleteByDeviceId(String deviceId);
}
