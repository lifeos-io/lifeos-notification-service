package com.lifeos.notification.device.entity;

import com.lifeos.base.id.UuidV7;
import com.lifeos.notification.common.persistence.AssignedIdEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A device that receives push for a user. A token moves to whoever registered it last (e.g. a shared phone). */
@Entity
@Table(name = "device")
public class Device extends AssignedIdEntity {

    @Id
    private UUID id;

    private UUID userId;
    private String token;

    @Enumerated(EnumType.STRING)
    private Platform platform;

    private Instant createdAt;
    private Instant lastSeenAt;

    protected Device() {
    }

    public static Device register(UUID userId, String token, Platform platform, Instant now) {
        Device device = new Device();
        device.id = UuidV7.next();
        device.userId = Objects.requireNonNull(userId);
        device.token = Objects.requireNonNull(token);
        device.platform = Objects.requireNonNull(platform);
        device.createdAt = now;
        device.lastSeenAt = now;
        return device;
    }

    /** Registered again, possibly by another user on the same device. */
    public void reregister(UUID userId, Platform platform, Instant now) {
        this.userId = Objects.requireNonNull(userId);
        this.platform = Objects.requireNonNull(platform);
        this.lastSeenAt = now;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
