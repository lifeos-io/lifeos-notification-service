package com.lifeos.notification.notification.dto;

import com.lifeos.notification.notification.entity.NotificationKind;
import java.time.Instant;
import java.util.UUID;

/**
 * A message in the inbox, in the language the user had when it arrived.
 *
 * @param subjectId the decision, weekly plan or goal it is about
 * @param link      where the app opens it
 */
public record NotificationResponse(
        UUID id,
        NotificationKind kind,
        UUID subjectId,
        String title,
        String body,
        String link,
        Instant readAt,
        Instant createdAt) {
}
