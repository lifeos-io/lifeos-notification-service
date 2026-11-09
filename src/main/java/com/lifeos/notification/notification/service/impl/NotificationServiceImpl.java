package com.lifeos.notification.notification.service.impl;

import com.lifeos.base.error.CommonErrorCode;
import com.lifeos.base.error.LifeOsException;
import com.lifeos.notification.delivery.entity.Channel;
import com.lifeos.notification.delivery.service.DeliveryService;
import com.lifeos.notification.device.repository.DeviceRepository;
import com.lifeos.notification.notification.dto.NotificationResponse;
import com.lifeos.notification.notification.entity.Notification;
import com.lifeos.notification.notification.entity.NotificationKind;
import com.lifeos.notification.notification.mapper.NotificationMapper;
import com.lifeos.notification.notification.mapper.NotificationTextMapper.Texts;
import com.lifeos.notification.notification.mapper.NotificationTextMapper;
import com.lifeos.notification.notification.repository.NotificationRepository;
import com.lifeos.notification.notification.service.NotificationService;
import com.lifeos.notification.recipient.dto.RecipientResponse;
import com.lifeos.notification.recipient.service.RecipientService;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the notification and schedules its deliveries in the transaction that handles the event; sending happens
 * later, apart from it. Push needs a registered device; e-mail needs the user to have turned it on.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final DeviceRepository deviceRepository;
    private final RecipientService recipientService;
    private final DeliveryService deliveryService;
    private final NotificationTextMapper notificationTextMapper;
    private final NotificationMapper notificationMapper;
    private final Clock clock;

    public NotificationServiceImpl(NotificationRepository repository, DeviceRepository deviceRepository,
            RecipientService recipientService, DeliveryService deliveryService,
            NotificationTextMapper notificationTextMapper, NotificationMapper notificationMapper, Clock clock) {
        this.repository = repository;
        this.deviceRepository = deviceRepository;
        this.recipientService = recipientService;
        this.deliveryService = deliveryService;
        this.notificationTextMapper = notificationTextMapper;
        this.notificationMapper = notificationMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void notify(UUID userId, UUID sourceEventId, NotificationKind kind, UUID subjectId, String link,
            Object... args) {
        if (repository.existsBySourceEventId(sourceEventId)) {
            return;
        }
        RecipientResponse recipient = recipientService.getOrCreate(userId);
        Texts texts = notificationTextMapper.texts(kind, recipient.locale(), args);
        Instant now = clock.instant();
        Notification notification = Notification.of(userId, kind, sourceEventId, subjectId, texts.title(),
                texts.body(), link, now);
        repository.save(notification);

        Set<Channel> channels = EnumSet.noneOf(Channel.class);
        if (recipient.pushEnabled() && deviceRepository.existsByUserId(userId)) {
            channels.add(Channel.PUSH);
        }
        if (recipient.emailEnabled()) {
            channels.add(Channel.EMAIL);
        }
        deliveryService.schedule(notification.getId(), userId, channels);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID userId, boolean unreadOnly) {
        return repository.search(userId, unreadOnly).stream().map(notificationMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new LifeOsException(CommonErrorCode.NOT_FOUND, "Notification not found."));
        notification.markRead(clock.instant());
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId, clock.instant());
    }
}
