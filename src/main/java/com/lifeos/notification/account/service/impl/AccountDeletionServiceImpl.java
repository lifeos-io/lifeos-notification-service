package com.lifeos.notification.account.service.impl;

import com.lifeos.notification.account.service.AccountDeletionService;
import com.lifeos.notification.delivery.repository.DeliveryRepository;
import com.lifeos.notification.device.repository.DeviceRepository;
import com.lifeos.notification.notification.repository.NotificationRepository;
import com.lifeos.notification.recipient.repository.RecipientRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes the user's pending and past deliveries (so nothing is sent after the deletion), inbox, devices and
 * settings, e-mail address included. Repeating it is harmless.
 */
@Service
public class AccountDeletionServiceImpl implements AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionServiceImpl.class);

    private final DeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final DeviceRepository deviceRepository;
    private final RecipientRepository recipientRepository;

    public AccountDeletionServiceImpl(DeliveryRepository deliveryRepository,
            NotificationRepository notificationRepository, DeviceRepository deviceRepository,
            RecipientRepository recipientRepository) {
        this.deliveryRepository = deliveryRepository;
        this.notificationRepository = notificationRepository;
        this.deviceRepository = deviceRepository;
        this.recipientRepository = recipientRepository;
    }

    @Override
    @Transactional
    public void deleteAll(UUID userId) {
        int deliveries = deliveryRepository.deleteByUserId(userId);
        int notifications = notificationRepository.deleteByUserId(userId);
        int devices = deviceRepository.deleteByUserId(userId);
        recipientRepository.deleteById(userId);
        log.info("Deleted the data of user {}: {} notifications, {} deliveries, {} devices", userId, notifications,
                deliveries, devices);
    }
}
