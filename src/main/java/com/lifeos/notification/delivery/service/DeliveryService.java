package com.lifeos.notification.delivery.service;

import com.lifeos.notification.delivery.entity.Channel;
import java.util.Set;
import java.util.UUID;

/** Sending notifications out of the app (push, e-mail), apart from receiving the events that cause them. */
public interface DeliveryService {

    /** A PENDING delivery per channel, due right away. */
    void schedule(UUID notificationId, UUID userId, Set<Channel> channels);
}
