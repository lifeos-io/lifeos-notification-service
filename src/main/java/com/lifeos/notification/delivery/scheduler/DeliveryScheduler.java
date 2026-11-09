package com.lifeos.notification.delivery.scheduler;

import com.lifeos.notification.delivery.service.DeliveryRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sends due deliveries every 30 seconds, batch after batch until none are left. */
@Component
public class DeliveryScheduler {

    static final int BATCH_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(DeliveryScheduler.class);

    private final DeliveryRunService deliveryRunService;

    public DeliveryScheduler(DeliveryRunService deliveryRunService) {
        this.deliveryRunService = deliveryRunService;
    }

    @Scheduled(fixedDelayString = "${lifeos.notification.sender.interval-ms:30000}",
            initialDelayString = "${lifeos.notification.sender.initial-delay-ms:10000}")
    public void sendDue() {
        try {
            int sent;
            do {
                sent = deliveryRunService.runDue(BATCH_SIZE);
            } while (sent == BATCH_SIZE);
        } catch (RuntimeException e) {
            log.error("Sending due deliveries failed; trying again on the next pass", e);
        }
    }
}
