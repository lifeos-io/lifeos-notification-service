package com.lifeos.notification.delivery.entity;

import com.lifeos.base.id.UuidV7;
import com.lifeos.notification.common.persistence.AssignedIdEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sending one notification on one channel. A failed attempt is tried again at {@code nextAttemptAt}; after the
 * last attempt the delivery is FAILED (the message is still in the inbox).
 */
@Entity
@Table(name = "delivery")
public class Delivery extends AssignedIdEntity {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    private UUID id;

    private UUID notificationId;
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private int attempts;
    private Instant nextAttemptAt;
    private String lastError;
    private Instant sentAt;

    @Version
    private int version;

    private Instant createdAt;
    private Instant updatedAt;

    protected Delivery() {
    }

    /** Due right away. */
    public static Delivery pending(UUID notificationId, UUID userId, Channel channel, Instant now) {
        Delivery delivery = new Delivery();
        delivery.id = UuidV7.next();
        delivery.notificationId = Objects.requireNonNull(notificationId);
        delivery.userId = Objects.requireNonNull(userId);
        delivery.channel = Objects.requireNonNull(channel);
        delivery.status = DeliveryStatus.PENDING;
        delivery.nextAttemptAt = now;
        delivery.createdAt = now;
        delivery.updatedAt = now;
        return delivery;
    }

    public void sent(Instant now) {
        this.attempts++;
        this.status = DeliveryStatus.SENT;
        this.sentAt = now;
        this.lastError = null;
        this.updatedAt = now;
    }

    /** An attempt failed: try again at {@code retryAt}, or give up when {@code retryAt} is null. */
    public void attemptFailed(String error, Instant retryAt, Instant now) {
        this.attempts++;
        this.lastError = error == null ? null
                : error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
        if (retryAt == null) {
            this.status = DeliveryStatus.FAILED;
        } else {
            this.nextAttemptAt = retryAt;
        }
        this.updatedAt = now;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Channel getChannel() {
        return channel;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
