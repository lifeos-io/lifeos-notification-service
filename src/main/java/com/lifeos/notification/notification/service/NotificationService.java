package com.lifeos.notification.notification.service;

import com.lifeos.notification.notification.dto.NotificationResponse;
import com.lifeos.notification.notification.entity.NotificationKind;
import java.util.List;
import java.util.UUID;

/**
 * The user's notifications: written into the in-app inbox in their language, and handed to delivery on the
 * channels they want. One notification per source event.
 */
public interface NotificationService {

    /**
     * @param sourceEventId the event this comes from (a redelivered or repeated event adds nothing)
     * @param subjectId     the decision, weekly plan or goal it is about
     * @param link          where the app opens it, e.g. {@code lifeos://decisions/<id>}
     * @param args          the message's arguments ({@code LocalDate}s are written in the user's language)
     */
    void notify(UUID userId, UUID sourceEventId, NotificationKind kind, UUID subjectId, String link, Object... args);

    /** Newest first; with {@code unreadOnly}, only those not read yet. */
    List<NotificationResponse> list(UUID userId, boolean unreadOnly);

    NotificationResponse markRead(UUID userId, UUID notificationId);

    /** @return how many were unread */
    int markAllRead(UUID userId);
}
