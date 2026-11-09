package com.lifeos.notification.delivery.service.impl;

import com.lifeos.notification.delivery.entity.Channel;
import com.lifeos.notification.delivery.entity.Delivery;
import com.lifeos.notification.delivery.repository.DeliveryRepository;
import com.lifeos.notification.delivery.service.DeliveryService;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository repository;
    private final Clock clock;

    public DeliveryServiceImpl(DeliveryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void schedule(UUID notificationId, UUID userId, Set<Channel> channels) {
        Instant now = clock.instant();
        for (Channel channel : channels) {
            repository.save(Delivery.pending(notificationId, userId, channel, now));
        }
    }
}
