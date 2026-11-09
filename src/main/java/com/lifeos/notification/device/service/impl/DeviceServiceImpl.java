package com.lifeos.notification.device.service.impl;

import com.lifeos.base.error.CommonErrorCode;
import com.lifeos.base.error.LifeOsException;
import com.lifeos.notification.device.dto.RegisterDeviceRequest;
import com.lifeos.notification.device.entity.Device;
import com.lifeos.notification.device.repository.DeviceRepository;
import com.lifeos.notification.device.service.DeviceService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository repository;
    private final Clock clock;

    public DeviceServiceImpl(DeviceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void register(UUID userId, RegisterDeviceRequest request) {
        Instant now = clock.instant();
        String token = request.token().strip();
        repository.findByToken(token).ifPresentOrElse(
                device -> device.reregister(userId, request.platform(), now),
                () -> repository.save(Device.register(userId, token, request.platform(), now)));
    }

    @Override
    @Transactional
    public void unregister(UUID userId, String token) {
        Device device = repository.findByToken(token)
                .filter(found -> found.getUserId().equals(userId))
                .orElseThrow(() -> new LifeOsException(CommonErrorCode.NOT_FOUND, "Device not found."));
        repository.delete(device);
    }
}
