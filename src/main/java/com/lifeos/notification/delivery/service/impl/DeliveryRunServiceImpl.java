package com.lifeos.notification.delivery.service.impl;

import com.lifeos.notification.delivery.client.EmailSender;
import com.lifeos.notification.delivery.client.PushSender;
import com.lifeos.notification.delivery.entity.Delivery;
import com.lifeos.notification.delivery.repository.DeliveryRepository;
import com.lifeos.notification.delivery.service.DeliveryRunService;
import com.lifeos.notification.device.entity.Device;
import com.lifeos.notification.device.repository.DeviceRepository;
import com.lifeos.notification.notification.entity.Notification;
import com.lifeos.notification.notification.repository.NotificationRepository;
import com.lifeos.notification.recipient.entity.Recipient;
import com.lifeos.notification.recipient.repository.RecipientRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Claims due deliveries with {@code FOR UPDATE SKIP LOCKED}, so replicas never send the same one twice. A failed
 * attempt is tried again after 1 min, 5 min, 30 min and 2 h; the fifth failure is final (the message stays in
 * the inbox). The user's preferences and devices are read again at sending time: e-mail turned off or no device
 * left ends the delivery without sending.
 */
@Service
public class DeliveryRunServiceImpl implements DeliveryRunService {

    /** Pauses after the 1st, 2nd, 3rd and 4th failed attempt; the 5th failure is final. */
    static final List<Duration> RETRY_PAUSES = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
            Duration.ofMinutes(30), Duration.ofHours(2));

    private static final Logger log = LoggerFactory.getLogger(DeliveryRunServiceImpl.class);

    private final DeliveryRepository repository;
    private final NotificationRepository notificationRepository;
    private final RecipientRepository recipientRepository;
    private final DeviceRepository deviceRepository;
    private final PushSender pushSender;
    private final EmailSender emailSender;
    private final Clock clock;

    public DeliveryRunServiceImpl(DeliveryRepository repository, NotificationRepository notificationRepository,
            RecipientRepository recipientRepository, DeviceRepository deviceRepository, PushSender pushSender,
            EmailSender emailSender, Clock clock) {
        this.repository = repository;
        this.notificationRepository = notificationRepository;
        this.recipientRepository = recipientRepository;
        this.deviceRepository = deviceRepository;
        this.pushSender = pushSender;
        this.emailSender = emailSender;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int runDue(int limit) {
        Instant now = clock.instant();
        List<Delivery> due = repository.claimDue(now, limit);
        for (Delivery delivery : due) {
            try {
                send(delivery);
                delivery.sent(now);
            } catch (DeliveryNotWanted e) {
                delivery.attemptFailed(e.getMessage(), null, now);
            } catch (RuntimeException e) {
                Instant retryAt = delivery.getAttempts() < RETRY_PAUSES.size()
                        ? now.plus(RETRY_PAUSES.get(delivery.getAttempts())) : null;
                log.warn("{} delivery {} failed (attempt {}){}", delivery.getChannel(), delivery.getId(),
                        delivery.getAttempts() + 1, retryAt == null ? ", giving up" : ", retrying at " + retryAt, e);
                delivery.attemptFailed(e.getClass().getSimpleName() + ": " + e.getMessage(), retryAt, now);
            }
        }
        return due.size();
    }

    private void send(Delivery delivery) {
        Notification notification = notificationRepository.findById(delivery.getNotificationId()).orElseThrow();
        Recipient recipient = recipientRepository.findById(delivery.getUserId())
                .orElseThrow(() -> new DeliveryNotWanted("recipient is gone"));
        switch (delivery.getChannel()) {
            case PUSH -> {
                List<String> tokens = deviceRepository.findByUserId(delivery.getUserId()).stream()
                        .map(Device::getToken)
                        .toList();
                if (!recipient.isPushEnabled() || tokens.isEmpty()) {
                    throw new DeliveryNotWanted("push is off or no device is registered");
                }
                pushSender.send(tokens, notification.getTitle(), notification.getBody(), notification.getLink());
            }
            case EMAIL -> {
                if (!recipient.isEmailEnabled()) {
                    throw new DeliveryNotWanted("e-mail is off");
                }
                String text = notification.getBody()
                        + (notification.getLink() == null ? "" : "\n\n" + notification.getLink());
                emailSender.send(recipient.getEmail(), notification.getTitle(), text);
            }
        }
    }

    /** Nothing to send any more (the user changed their mind); final, not retried. */
    private static final class DeliveryNotWanted extends RuntimeException {

        DeliveryNotWanted(String reason) {
            super(reason, null, false, false);
        }
    }
}
