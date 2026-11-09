package com.lifeos.notification.device.controller;

import com.lifeos.base.web.security.CurrentUser;
import com.lifeos.notification.device.dto.RegisterDeviceRequest;
import com.lifeos.notification.device.service.DeviceService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/notifications/devices}: the signed-in user's push devices. */
@RestController
@RequestMapping("/api/notifications/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** Registers (or re-registers) this device; 204. */
    @PostMapping
    public ResponseEntity<Void> register(@CurrentUser UUID userId, @Valid @RequestBody RegisterDeviceRequest request) {
        deviceService.register(userId, request);
        return ResponseEntity.noContent().build();
    }

    /** Only the user's own device; another user's token is reported as missing. */
    @DeleteMapping("/{token}")
    public ResponseEntity<Void> unregister(@CurrentUser UUID userId, @PathVariable String token) {
        deviceService.unregister(userId, token);
        return ResponseEntity.noContent().build();
    }
}
