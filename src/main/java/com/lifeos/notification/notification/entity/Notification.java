package com.lifeos.notification.notification.entity;

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

/**
 * A message in the user's in-app inbox, written in their language when the event arrived. One per source event.
 * Only {@code readAt} changes afterwards.
 */
@Entity
@Table(name = "notification")
public class Notification extends AssignedIdEntity {

    @Id
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private NotificationKind kind;

    private UUID sourceEventId;
    private UUID subjectId;
    private String title;
    private String body;
    private String link;
    private Instant readAt;
    private Instant createdAt;

    protected Notification() {
    }

    public static Notification of(UUID userId, NotificationKind kind, UUID sourceEventId, UUID subjectId,
            String title, String body, String link, Instant now) {
        Notification notification = new Notification();
        notification.id = UuidV7.next();
        notification.userId = Objects.requireNonNull(userId);
        notification.kind = Objects.requireNonNull(kind);
        notification.sourceEventId = Objects.requireNonNull(sourceEventId);
        notification.subjectId = subjectId;
        notification.title = Objects.requireNonNull(title);
        notification.body = Objects.requireNonNull(body);
        notification.link = link;
        notification.createdAt = now;
        return notification;
    }

    /** @return whether it was unread */
    public boolean markRead(Instant now) {
        if (readAt != null) {
            return false;
        }
        this.readAt = now;
        return true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationKind getKind() {
        return kind;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getLink() {
        return link;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
