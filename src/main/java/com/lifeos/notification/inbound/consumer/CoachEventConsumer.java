package com.lifeos.notification.inbound.consumer;

import com.lifeos.base.messaging.MalformedEventException;
import com.lifeos.base.messaging.inbox.Inbox;
import com.lifeos.notification.inbound.event.RecommendationProposedV1;
import com.lifeos.notification.notification.entity.NotificationKind;
import com.lifeos.notification.notification.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

/** Coach's events: a proposal waiting for the user's decision (stored and decided in Strategy). */
@Component
public class CoachEventConsumer {

    private final Inbox inbox;
    private final NotificationService notificationService;

    public CoachEventConsumer(Inbox inbox, NotificationService notificationService) {
        this.inbox = inbox;
        this.notificationService = notificationService;
    }

    @RetryableTopic(exclude = MalformedEventException.class, retryTopicSuffix = "-notification-retry",
            dltTopicSuffix = "-notification-dlt")
    @KafkaListener(topics = "${lifeos.notification.coach-topic:lifeos.coach.events.v1}", groupId = "notification")
    public void on(ConsumerRecord<String, String> record) {
        inbox.handle(record, event -> {
            if (event.is(RecommendationProposedV1.TYPE, 1)) {
                RecommendationProposedV1 proposal = event.payloadAs(RecommendationProposedV1.class);
                notificationService.notify(event.userId(), event.envelope().eventId(),
                        NotificationKind.PROPOSAL_RECEIVED, proposal.goalId(), Links.proposals(proposal.goalId()),
                        proposal.rationale());
            }
        });
    }
}
