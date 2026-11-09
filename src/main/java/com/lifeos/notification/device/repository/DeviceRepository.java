package com.lifeos.notification.device.repository;

import com.lifeos.notification.device.entity.Device;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByToken(String token);

    List<Device> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM Device d WHERE d.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}
