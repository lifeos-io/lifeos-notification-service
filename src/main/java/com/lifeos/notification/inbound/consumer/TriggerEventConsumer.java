package com.lifeos.notification.inbound.consumer;

import com.lifeos.base.messaging.MalformedEventException;
import com.lifeos.base.messaging.inbox.Inbox;
import com.lifeos.notification.inbound.event.DecisionReviewDueV1;
import com.lifeos.notification.inbound.event.DriftDetectedV1;
import com.lifeos.notification.inbound.event.ReopenTriggerFiredV1;
import com.lifeos.notification.inbound.event.WeeklyReviewDueV1;
import com.lifeos.notification.notification.entity.NotificationKind;
import com.lifeos.notification.notification.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

/** Trigger's events, each one a reminder for the user. */
@Component
public class TriggerEventConsumer {

    private final Inbox inbox;
    private final NotificationService notificationService;

    public TriggerEventConsumer(Inbox inbox, NotificationService notificationService) {
        this.inbox = inbox;
        this.notificationService = notificationService;
    }

    @RetryableTopic(exclude = MalformedEventException.class, retryTopicSuffix = "-notification-retry",
            dltTopicSuffix = "-notification-dlt")
    @KafkaListener(topics = "${lifeos.notification.trigger-topic:lifeos.trigger.events.v1}",
            groupId = "notification")
    public void on(ConsumerRecord<String, String> record) {
        inbox.handle(record, event -> {
            if (event.eventVersion() != 1) {
                return;
            }
            var userId = event.userId();
            var eventId = event.envelope().eventId();
            switch (event.eventType()) {
                case DecisionReviewDueV1.TYPE -> {
                    DecisionReviewDueV1 due = event.payloadAs(DecisionReviewDueV1.class);
                    notificationService.notify(userId, eventId, NotificationKind.DECISION_REVIEW_DUE,
                            due.decisionId(), Links.decision(due.decisionId()), due.title(), due.reviewOn());
                }
                case ReopenTriggerFiredV1.TYPE -> {
                    ReopenTriggerFiredV1 fired = event.payloadAs(ReopenTriggerFiredV1.class);
                    notificationService.notify(userId, eventId, NotificationKind.REOPEN_TRIGGER_FIRED,
                            fired.decisionId(), Links.decision(fired.decisionId()), fired.description());
                }
                case WeeklyReviewDueV1.TYPE -> {
                    WeeklyReviewDueV1 due = event.payloadAs(WeeklyReviewDueV1.class);
                    notificationService.notify(userId, eventId, NotificationKind.WEEKLY_REVIEW_DUE,
                            due.weeklyPlanId(), Links.weeklyReview(due.isoWeek()), due.isoWeek());
                }
                case DriftDetectedV1.TYPE -> {
                    DriftDetectedV1 drift = event.payloadAs(DriftDetectedV1.class);
                    notificationService.notify(userId, eventId, NotificationKind.DRIFT_DETECTED,
                            drift.weeklyPlanId(), Links.weeklyPlan(drift.isoWeek()), drift.isoWeek(),
                            drift.plannedMinutes(), drift.doneMinutes());
                }
                default -> {
                    // A Trigger event that is not a message for the user.
                }
            }
        });
    }
}
