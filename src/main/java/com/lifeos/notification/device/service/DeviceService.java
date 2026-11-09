package com.lifeos.notification.device.service;

import com.lifeos.notification.device.dto.RegisterDeviceRequest;
import java.util.UUID;

/** The devices that receive a user's push. */
public interface DeviceService {

    /** Registers the token for the user; a token registered by someone else moves to this user. */
    void register(UUID userId, RegisterDeviceRequest request);

    /** Unregisters one of the user's own devices (e.g. on sign-out). */
    void unregister(UUID userId, String token);
}
