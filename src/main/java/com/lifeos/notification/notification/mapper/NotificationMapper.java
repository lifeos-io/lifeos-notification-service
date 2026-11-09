package com.lifeos.notification.notification.mapper;

import com.lifeos.notification.notification.dto.NotificationResponse;
import com.lifeos.notification.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getKind(), notification.getSubjectId(),
                notification.getTitle(), notification.getBody(), notification.getLink(), notification.getReadAt(),
                notification.getCreatedAt());
    }
}
