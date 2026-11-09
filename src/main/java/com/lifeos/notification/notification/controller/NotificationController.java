package com.lifeos.notification.notification.controller;

import com.lifeos.base.web.security.CurrentUser;
import com.lifeos.notification.notification.dto.NotificationResponse;
import com.lifeos.notification.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/notifications}: the signed-in user's in-app inbox. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Newest first; {@code ?unread=true} keeps the unread ones. */
    @GetMapping
    public List<NotificationResponse> list(@CurrentUser UUID userId,
            @RequestParam(defaultValue = "false") boolean unread) {
        return notificationService.list(userId, unread);
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponse markRead(@CurrentUser UUID userId, @PathVariable UUID notificationId) {
        return notificationService.markRead(userId, notificationId);
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@CurrentUser UUID userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
