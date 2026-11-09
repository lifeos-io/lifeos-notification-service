package com.lifeos.notification.device.dto;

import com.lifeos.notification.device.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code POST /api/notifications/devices}: this device should receive the signed-in user's push. */
public record RegisterDeviceRequest(
        @NotBlank @Size(max = 4096) String token,
        @NotNull Platform platform) {
}
